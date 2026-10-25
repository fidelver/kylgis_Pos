// KylGis POS Punto de Venta Táctil - GPLv3+
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.Transaction;
import com.mx.kylgis.pos.inventory.MovementReason;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Atomic recipe consumption at sale finalization. Shortages never reject a sale. */
public final class SaleRecipeConsumptionService {
    private static final String CONSUMPTION = "SALE_RECIPE_CONSUMPTION";
    private static final String REVERSAL = "SALE_RECIPE_REVERSAL";
    private static final String PENDING = "PENDING";
    private static final String POSTED = "POSTED";
    // Directions describe the warehouse movement: INPUT adds stock, OUTPUT removes it.
    private static final String INPUT = "INPUT";
    private static final String OUTPUT = "OUTPUT";
    private static final int SCALE = 6;
    private final Session session;

    public SaleRecipeConsumptionService(Session session) {
        if (session == null) throw new IllegalArgumentException("session is required");
        this.session = session;
    }

    /** Recalculates the cost of every active sale-consumption recipe. */
    public static void refreshActiveRecipeCosts(Connection connection) throws BasicException {
        if (connection == null) {
            throw new BasicException("A database connection is required to refresh recipe costs.");
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, product_id, yield_quantity, yield_uom_id "
                + "FROM recipe_versions WHERE UPPER(status)='ACTIVE' "
                + "AND UPPER(recipe_mode)='SALE_CONSUMPTION'")) {
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    refreshRecipeCost(connection, new Recipe(rows.getString(1), rows.getString(2),
                            rows.getBigDecimal(3), rows.getString(4)));
                }
            }
            resolveSatisfiedAlerts(connection);
        } catch (SQLException ex) {
            throw new BasicException("Unable to refresh active sale recipe costs.", ex);
        }
    }

    /** Marks pending alerts as resolved once the required stock is available. */
    public static void resolveSatisfiedAlerts(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE sale_recipe_consumption_alerts a SET status='RESOLVED', "
                + "resolved_at=CURRENT_TIMESTAMP, resolved_by='SYSTEM' "
                + "WHERE a.status='PENDING' AND EXISTS ("
                + "SELECT 1 FROM stockcurrent sc WHERE sc.location=a.location_id "
                + "AND sc.product=a.product_id AND sc.attributesetinstance_id IS NULL "
                + "AND sc.units >= a.required_quantity)")) {
            ps.executeUpdate();
        }
    }

    /** Number of unresolved shortages/conversion warnings visible to inventory managers. */
    public int pendingAlertCount() throws BasicException {
        return new Transaction<Integer>(session) {
            @Override protected Integer transact() throws BasicException {
                try {
                    resolveSatisfiedAlerts(session.getConnection());
                } catch (SQLException ex) {
                    throw new BasicException("Unable to resolve satisfied sale recipe alerts.", ex);
                }
                try (PreparedStatement ps = session.getConnection().prepareStatement(
                        "SELECT COUNT(*) FROM sale_recipe_consumption_alerts WHERE status=?")) {
                    ps.setString(1, PENDING);
                    try (ResultSet row = ps.executeQuery()) {
                        return row.next() ? row.getInt(1) : 0;
                    }
                } catch (SQLException ex) {
                    throw new BasicException("Unable to read pending sale recipe alerts.", ex);
                }
            }
        }.execute();
    }

    public ConsumptionResult consume(final SaleConsumptionRequest request) throws BasicException {
        validate(request);
        return new Transaction<ConsumptionResult>(session) {
            @Override protected ConsumptionResult transact() throws BasicException {
                try {
                    Connection c = session.getConnection();
                    refreshActiveRecipeCosts(c);
                    String hash = hash(request);
                    ConsumptionResult existing = existingConsumption(c, request.idempotencyKey, hash);
                    if (existing != null) return existing;
                    Plan plan = new Plan();
                    for (SaleLine line : request.lines) {
                        if (line == null || line.service || line.productId == null
                                || line.productId.isBlank() || line.quantity == null
                                || line.quantity.signum() <= 0) continue;
                        expand(c, line.productId, decimal(line.quantity), new HashSet<>(), plan);
                    }
                    if (plan.lines.isEmpty() && plan.alerts.isEmpty()) {
                        return new ConsumptionResult(null, 0, 0, false);
                    }
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    String id = UUID.randomUUID().toString();
                    insertHeader(c, id, request, hash, now,
                            "receipt=" + request.receiptId + ";recipes=" + String.join(",", plan.recipes));
                    insertLink(c, request, id, now);
                    int no = 1;
                    for (PlanLine line : plan.lines.values()) {
                        Product product = product(c, line.productId);
                        BigDecimal available = lockStock(c, request.locationId, line.productId);
                        if (available.signum() < 0 || available.compareTo(line.quantity) < 0) {
                            alert(c, request, line, available, now,
                                    available.signum() < 0 ? "NEGATIVE_STOCK" : "SHORTAGE");
                        }
                        delta(c, request.locationId, line.productId, line.quantity.negate());
                        String diary = diary(c, now, ((Number) MovementReason.OUT_SALE.getKey()).intValue(),
                                request.locationId, line.productId, line.quantity.negate(),
                                product.cost, request.createdBy);
                        line(c, id, no++, OUTPUT, line.productId, line.quantity,
                                product.uom, line.quantity, product.cost,
                                money(product.cost.multiply(line.quantity)), diary);
                    }
                    for (PlanAlert missing : plan.alerts) {
                        alert(c, request, missing.line, missing.available, now, missing.kind);
                    }
                    post(c, id);
                    return new ConsumptionResult(id, plan.lines.size(), plan.alerts.size(), false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to post sale recipe consumption.", ex);
                }
            }
        }.execute();
    }

    public ReversalResult reverseForRefund(final RefundRequest request) throws BasicException {
        validate(request);
        return new Transaction<ReversalResult>(session) {
            @Override protected ReversalResult transact() throws BasicException {
                try {
                    Connection c = session.getConnection();
                    String receipt = receiptForTicket(c, request.sourceTicketId);
                    if (receipt == null) return new ReversalResult(null, null, false);
                    Original original = original(c, receipt);
                    if (original == null) return new ReversalResult(null, null, false);
                    String hash = sha256(request.sourceTicketId + "|" + original.id + "|" + request.createdBy);
                    ReversalResult replay = existingReversal(c, request.idempotencyKey, hash);
                    if (replay != null) return replay;
                    List<StoredLine> lines = lines(c, original.id);
                    if (lines.isEmpty()) return new ReversalResult(null, original.id, false);
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    String reversalId = UUID.randomUUID().toString();
                    insertReversal(c, reversalId, request, original, hash, now);
                    int no = 1;
                    for (StoredLine stored : lines) {
                        Product product = product(c, stored.productId);
                        delta(c, original.location, stored.productId, stored.baseQuantity);
                        String diary = diary(c, now, ((Number) MovementReason.IN_REFUND.getKey()).intValue(), original.location,
                                stored.productId, stored.baseQuantity, product.cost, request.createdBy);
                        line(c, reversalId, no++, INPUT, stored.productId, stored.quantity,
                                stored.uom, stored.baseQuantity, stored.unitCost, stored.totalCost, diary);
                    }
                    post(c, reversalId);
                    return new ReversalResult(reversalId, original.id, false);
                } catch (SQLException ex) {
                    throw new BasicException("Unable to reverse sale recipe consumption.", ex);
                }
            }
        }.execute();
    }

    public static final class SaleLine {
        public final int lineNo; public final String productId; public final BigDecimal quantity; public final boolean service;
        public SaleLine(int lineNo, String productId, BigDecimal quantity, boolean service) {
            this.lineNo = lineNo; this.productId = productId; this.quantity = quantity; this.service = service;
        }
    }
    public static final class SaleConsumptionRequest {
        public final String idempotencyKey, receiptId, locationId, createdBy; public final int ticketId; public final List<SaleLine> lines;
        public SaleConsumptionRequest(String key, String receipt, int ticket, String location, List<SaleLine> lines, String user) {
            idempotencyKey = key; receiptId = receipt; ticketId = ticket; locationId = location; this.lines = lines == null ? null : new ArrayList<>(lines); createdBy = user;
        }
    }
    public static final class RefundRequest {
        public final String idempotencyKey, createdBy; public final int sourceTicketId;
        public RefundRequest(String key, int sourceTicket, String user) { idempotencyKey = key; sourceTicketId = sourceTicket; createdBy = user; }
    }
    public static final class ConsumptionResult {
        public final String transformationId; public final int consumedProducts, alerts; public final boolean idempotentReplay;
        ConsumptionResult(String id, int products, int alerts, boolean replay) { transformationId = id; consumedProducts = products; this.alerts = alerts; idempotentReplay = replay; }
    }
    public static final class ReversalResult {
        public final String reversalTransformationId, originalTransformationId; public final boolean idempotentReplay;
        ReversalResult(String reversal, String original, boolean replay) { reversalTransformationId = reversal; originalTransformationId = original; idempotentReplay = replay; }
    }

    private static final class Plan { final Map<String, PlanLine> lines = new LinkedHashMap<>(); final List<PlanAlert> alerts = new ArrayList<>(); final Set<String> recipes = new HashSet<>(); }
    private static final class PlanLine { final String productId; BigDecimal quantity; PlanLine(String id, BigDecimal qty) { productId = id; quantity = qty; } }
    private static final class PlanAlert { final PlanLine line; final BigDecimal available; final String kind; PlanAlert(PlanLine l, BigDecimal a, String k) { line = l; available = a; kind = k; } }
    private static final class Recipe { final String id, productId; final BigDecimal yield; final String uom; Recipe(String i, String p, BigDecimal y, String u) { id = i; productId = p; yield = y; uom = u; } }
    private static final class Component { final String productId, uom; final BigDecimal quantity; Component(String p, BigDecimal q, String u) { productId = p; quantity = q; uom = u; } }
    private static final class Bundle { final String productId; final BigDecimal quantity; Bundle(String p, BigDecimal q) { productId = p; quantity = q; } }
    private static final class Product { final String uom; final BigDecimal cost; Product(String u, BigDecimal c) { uom = u; cost = money(c); } }
    private static final class StoredLine { final String productId, uom; final BigDecimal quantity, baseQuantity, unitCost, totalCost; StoredLine(String p, BigDecimal q, String u, BigDecimal b, BigDecimal c, BigDecimal t) { productId = p; quantity = q; uom = u; baseQuantity = b; unitCost = c; totalCost = t; } }
    private static final class Original { final String id, location; Original(String i, String l) { id = i; location = l; } }

    private void expand(Connection c, String productId, BigDecimal quantity, Set<String> path, Plan plan) throws SQLException, BasicException {
        if (!path.add(productId)) throw new BasicException("Bundle cycle detected at product " + productId);
        List<Bundle> bundle = bundles(c, productId);
        if (!bundle.isEmpty()) { for (Bundle item : bundle) expand(c, item.productId, quantity.multiply(item.quantity), path, plan); path.remove(productId); return; }
        Recipe recipe = recipe(c, productId);
        if (recipe == null) { path.remove(productId); return; }
        if (recipe.yield == null || recipe.yield.signum() <= 0) throw new BasicException("Active sale recipe has an invalid yield.");
        plan.recipes.add(recipe.id);
        Product result = product(c, productId);
        BigDecimal yieldQty = convert(c, productId, quantity, result.uom, recipe.uom, plan, productId);
        if (yieldQty == null) { path.remove(productId); return; }
        BigDecimal multiplier = yieldQty.divide(recipe.yield, 9, RoundingMode.HALF_UP);
        for (Component item : components(c, recipe.id)) {
            Product component = product(c, item.productId);
            BigDecimal base = convert(c, item.productId, item.quantity.multiply(multiplier), item.uom, component.uom, plan, item.productId);
            if (base == null || base.signum() <= 0) continue;
            PlanLine line = plan.lines.computeIfAbsent(item.productId, k -> new PlanLine(k, BigDecimal.ZERO));
            line.quantity = qty(line.quantity.add(base));
        }
        path.remove(productId);
    }

    private static void refreshRecipeCost(Connection c, Recipe recipe) throws SQLException, BasicException {
        if (recipe == null || recipe.yield == null || recipe.yield.signum() <= 0
                || recipe.uom == null || recipe.productId == null) {
            return;
        }
        Product output = product(c, recipe.productId);
        BigDecimal outputYield = convertForCost(c, recipe.productId, recipe.yield,
                recipe.uom, output.uom);
        if (outputYield == null || outputYield.signum() <= 0) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Component item : components(c, recipe.id)) {
            Product component = product(c, item.productId);
            BigDecimal base = convertForCost(c, item.productId, item.quantity,
                    item.uom, component.uom);
            if (base == null) {
                return;
            }
            total = total.add(base.multiply(component.cost));
        }
        BigDecimal unitCost = total.divide(outputYield, SCALE + 3, RoundingMode.HALF_UP);
        try (PreparedStatement update = c.prepareStatement(
                "UPDATE products SET stockcost=? WHERE id=?")) {
            update.setBigDecimal(1, money(unitCost));
            update.setString(2, recipe.productId);
            update.executeUpdate();
        }
    }

    private static BigDecimal convertForCost(Connection c, String productId,
            BigDecimal value, String from, String to) throws SQLException {
        if (from == null || to == null) {
            return null;
        }
        if (from.equals(to)) {
            return qty(value);
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT factor FROM product_uom_conversions "
                + "WHERE product_id=? AND from_uom_id=? AND to_uom_id=? AND active=?")) {
            ps.setString(1, productId);
            ps.setString(2, from);
            ps.setString(3, to);
            ps.setBoolean(4, true);
            try (ResultSet row = ps.executeQuery()) {
                if (!row.next() || row.getBigDecimal(1) == null
                        || row.getBigDecimal(1).signum() <= 0) {
                    return null;
                }
                return qty(value.multiply(row.getBigDecimal(1)));
            }
        }
    }

    private BigDecimal convert(Connection c, String productId, BigDecimal value, String from, String to, Plan plan, String alertProduct) throws SQLException {
        if (from.equals(to)) return qty(value);
        try (PreparedStatement ps = c.prepareStatement("SELECT factor FROM product_uom_conversions WHERE product_id=? AND from_uom_id=? AND to_uom_id=? AND active=?")) {
            ps.setString(1, productId); ps.setString(2, from); ps.setString(3, to); ps.setBoolean(4, true);
            try (ResultSet row = ps.executeQuery()) {
                if (!row.next()) { plan.alerts.add(new PlanAlert(new PlanLine(alertProduct, qty(value)), BigDecimal.ZERO, "MISSING_UOM_CONVERSION")); return null; }
                BigDecimal factor = row.getBigDecimal(1);
                if (factor == null || factor.signum() <= 0) { plan.alerts.add(new PlanAlert(new PlanLine(alertProduct, qty(value)), BigDecimal.ZERO, "INVALID_UOM_CONVERSION")); return null; }
                return qty(value.multiply(factor));
            }
        }
    }
    private static List<Bundle> bundles(Connection c, String product) throws SQLException { List<Bundle> r = new ArrayList<>(); try (PreparedStatement ps = c.prepareStatement("SELECT PRODUCT_BUNDLE, QUANTITY FROM products_bundle WHERE PRODUCT=?")) { ps.setString(1, product); try (ResultSet rows = ps.executeQuery()) { while (rows.next()) r.add(new Bundle(rows.getString(1), decimal(rows.getBigDecimal(2)))); }} return r; }
    private static Recipe recipe(Connection c, String product) throws SQLException { try (PreparedStatement ps = c.prepareStatement("SELECT id,product_id,yield_quantity,yield_uom_id FROM recipe_versions WHERE product_id=? AND UPPER(status)='ACTIVE' AND UPPER(recipe_mode)='SALE_CONSUMPTION'")) { ps.setString(1, product); try (ResultSet row = ps.executeQuery()) { return row.next() ? new Recipe(row.getString(1), row.getString(2), row.getBigDecimal(3), row.getString(4)) : null; }} }
    private static List<Component> components(Connection c, String recipe) throws SQLException { List<Component> r = new ArrayList<>(); try (PreparedStatement ps = c.prepareStatement("SELECT component_product_id,quantity,uom_id FROM recipe_components WHERE recipe_version_id=? ORDER BY line_no")) { ps.setString(1, recipe); try (ResultSet rows = ps.executeQuery()) { while (rows.next()) r.add(new Component(rows.getString(1), decimal(rows.getBigDecimal(2)), rows.getString(3))); }} return r; }
    private static Product product(Connection c, String id) throws SQLException, BasicException { try (PreparedStatement ps = c.prepareStatement("SELECT uom,CASE WHEN stockcost > 0 THEN stockcost ELSE COALESCE(pricebuy,0) END FROM products WHERE id=?")) { ps.setString(1, id); try (ResultSet row = ps.executeQuery()) { if (!row.next()) throw new BasicException("Unknown recipe product."); return new Product(row.getString(1), row.getBigDecimal(2)); }} }
    private static BigDecimal lockStock(Connection c, String location, String product) throws SQLException { try (PreparedStatement ps = c.prepareStatement("SELECT units FROM stockcurrent WHERE location=? AND product=? AND attributesetinstance_id IS NULL FOR UPDATE")) { ps.setString(1, location); ps.setString(2, product); try (ResultSet row = ps.executeQuery()) { return row.next() ? qty(row.getBigDecimal(1)) : BigDecimal.ZERO; }} }
    private static void delta(Connection c, String location, String product, BigDecimal value) throws SQLException { try (PreparedStatement ps = c.prepareStatement("UPDATE stockcurrent SET units=units+? WHERE location=? AND product=? AND attributesetinstance_id IS NULL")) { ps.setBigDecimal(1, value); ps.setString(2, location); ps.setString(3, product); if (ps.executeUpdate() == 0) try (PreparedStatement ins = c.prepareStatement("INSERT INTO stockcurrent(location,product,attributesetinstance_id,units) VALUES(?,?,NULL,?)")) { ins.setString(1, location); ins.setString(2, product); ins.setBigDecimal(3, value); ins.executeUpdate(); }} }
    private static String diary(Connection c, Timestamp time, int reason, String location, String product, BigDecimal units, BigDecimal cost, String user) throws SQLException { String id=UUID.randomUUID().toString(); try (PreparedStatement ps=c.prepareStatement("INSERT INTO stockdiary(id,datenew,reason,location,product,attributesetinstance_id,units,price,appuser,supplier,supplierdoc) VALUES(?,?,?,?,?,NULL,?,?,?,NULL,NULL)")) { ps.setString(1,id); ps.setTimestamp(2,time); ps.setInt(3,reason); ps.setString(4,location); ps.setString(5,product); ps.setBigDecimal(6,units); ps.setBigDecimal(7,cost); ps.setString(8,user); ps.executeUpdate(); } return id; }
    private static void insertHeader(Connection c, String id, SaleConsumptionRequest req, String hash, Timestamp time, String notes) throws SQLException { try (PreparedStatement ps=c.prepareStatement("INSERT INTO inventory_transformations(id,idempotency_key,request_hash,type,status,location_id,recipe_version_id,reverses_id,expected_output,actual_output,created_at,created_by,notes) VALUES(?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,?,?)")) { ps.setString(1,id); ps.setString(2,req.idempotencyKey); ps.setString(3,hash); ps.setString(4,CONSUMPTION); ps.setString(5,PENDING); ps.setString(6,req.locationId); ps.setTimestamp(7,time); ps.setString(8,req.createdBy); ps.setString(9,notes); ps.executeUpdate(); }}
    private static void insertReversal(Connection c, String id, RefundRequest req, Original original, String hash, Timestamp time) throws SQLException { try (PreparedStatement ps=c.prepareStatement("INSERT INTO inventory_transformations(id,idempotency_key,request_hash,type,status,location_id,recipe_version_id,reverses_id,expected_output,actual_output,created_at,created_by,notes) VALUES(?,?,?,?,?,?,NULL,?,NULL,NULL,?,?,?)")) { ps.setString(1,id); ps.setString(2,req.idempotencyKey); ps.setString(3,hash); ps.setString(4,REVERSAL); ps.setString(5,PENDING); ps.setString(6,original.location); ps.setString(7,original.id); ps.setTimestamp(8,time); ps.setString(9,req.createdBy); ps.setString(10,"refundOf="+original.id); ps.executeUpdate(); }}
    private static void insertLink(Connection c, SaleConsumptionRequest req, String id, Timestamp time) throws SQLException { try (PreparedStatement ps=c.prepareStatement("INSERT INTO sale_recipe_consumptions(id,receipt_id,ticket_id,transformation_id,location_id,created_at,created_by) VALUES(?,?,?,?,?,?,?)")) { ps.setString(1,UUID.randomUUID().toString()); ps.setString(2,req.receiptId); ps.setInt(3,req.ticketId); ps.setString(4,id); ps.setString(5,req.locationId); ps.setTimestamp(6,time); ps.setString(7,req.createdBy); ps.executeUpdate(); }}
    private static void alert(Connection c, SaleConsumptionRequest req, PlanLine line, BigDecimal available, Timestamp time, String kind) throws SQLException { String uom=""; try (PreparedStatement ps=c.prepareStatement("SELECT uom FROM products WHERE id=?")) { ps.setString(1,line.productId); try(ResultSet row=ps.executeQuery()){if(row.next()&&row.getString(1)!=null)uom=row.getString(1);}} try (PreparedStatement ps=c.prepareStatement("INSERT INTO sale_recipe_consumption_alerts(id,receipt_id,ticket_id,location_id,product_id,required_quantity,available_quantity,uom_id,kind,status,created_at) VALUES(?,?,?,?,?,?,?,?,?,'PENDING',?)")) { ps.setString(1,UUID.randomUUID().toString()); ps.setString(2,req.receiptId); ps.setInt(3,req.ticketId); ps.setString(4,req.locationId); ps.setString(5,line.productId); ps.setBigDecimal(6,line.quantity); ps.setBigDecimal(7,available==null?BigDecimal.ZERO:available); ps.setString(8,uom); ps.setString(9,kind); ps.setTimestamp(10,time); ps.executeUpdate(); }}
    private static void line(Connection c, String transformation, int no, String direction, String product, BigDecimal quantity, String uom, BigDecimal base, BigDecimal cost, BigDecimal total, String diary) throws SQLException { try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory_transformation_lines(transformation_id,line_no,direction,product_id,attributesetinstance_id,quantity,uom_id,base_quantity,unit_cost,total_cost,stockdiary_id) VALUES(?,?,?,?,NULL,?,?,?,?,?,?)")){ps.setString(1,transformation);ps.setInt(2,no);ps.setString(3,direction);ps.setString(4,product);ps.setBigDecimal(5,quantity);ps.setString(6,uom);ps.setBigDecimal(7,base);ps.setBigDecimal(8,cost);ps.setBigDecimal(9,total);ps.setString(10,diary);ps.executeUpdate();}}
    private static void post(Connection c, String id) throws SQLException { try(PreparedStatement ps=c.prepareStatement("UPDATE inventory_transformations SET status=? WHERE id=? AND status=?")){ps.setString(1,POSTED);ps.setString(2,id);ps.setString(3,PENDING);if(ps.executeUpdate()!=1)throw new SQLException("Transformation did not post.");}}
    private static ConsumptionResult existingConsumption(Connection c, String key, String hash) throws SQLException, BasicException { try(PreparedStatement ps=c.prepareStatement("SELECT id,request_hash,status FROM inventory_transformations WHERE idempotency_key=? FOR UPDATE")){ps.setString(1,key);try(ResultSet row=ps.executeQuery()){if(!row.next())return null;if(!hash.equals(row.getString(2)))throw new BasicException("Sale idempotency key is already used by another request.");if(!POSTED.equalsIgnoreCase(row.getString(3)))throw new BasicException("Existing sale consumption is not POSTED.");return new ConsumptionResult(row.getString(1),countLines(c,row.getString(1)),0,true);}}}
    private static ReversalResult existingReversal(Connection c, String key, String hash) throws SQLException, BasicException { try(PreparedStatement ps=c.prepareStatement("SELECT id,request_hash,status,reverses_id FROM inventory_transformations WHERE idempotency_key=? FOR UPDATE")){ps.setString(1,key);try(ResultSet row=ps.executeQuery()){if(!row.next())return null;if(!hash.equals(row.getString(2)))throw new BasicException("Refund idempotency key is already used by another request.");if(!POSTED.equalsIgnoreCase(row.getString(3)))throw new BasicException("Existing sale reversal is not POSTED.");return new ReversalResult(row.getString(1),row.getString(4),true);}}}
    private static int countLines(Connection c,String id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM inventory_transformation_lines WHERE transformation_id=?")){ps.setString(1,id);try(ResultSet row=ps.executeQuery()){return row.next()?row.getInt(1):0;}}}
    private static Original original(Connection c,String receipt)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT transformation_id,location_id FROM sale_recipe_consumptions WHERE receipt_id=? FOR UPDATE")){ps.setString(1,receipt);try(ResultSet row=ps.executeQuery()){return row.next()?new Original(row.getString(1),row.getString(2)):null;}}}
    private static List<StoredLine> lines(Connection c,String id)throws SQLException{List<StoredLine>r=new ArrayList<>();try(PreparedStatement ps=c.prepareStatement("SELECT l.product_id,l.quantity,l.uom_id,l.base_quantity,l.unit_cost,l.total_cost FROM inventory_transformation_lines l LEFT JOIN stockdiary sd ON sd.id=l.stockdiary_id WHERE l.transformation_id=? AND sd.units<0 ORDER BY l.line_no FOR UPDATE")){ps.setString(1,id);try(ResultSet rows=ps.executeQuery()){while(rows.next())r.add(new StoredLine(rows.getString(1),rows.getBigDecimal(2),rows.getString(3),rows.getBigDecimal(4),rows.getBigDecimal(5),rows.getBigDecimal(6)));}}return r;}
    private static String receiptForTicket(Connection c,int ticket)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT id FROM tickets WHERE tickettype=0 AND ticketid=?")){ps.setInt(1,ticket);try(ResultSet row=ps.executeQuery()){return row.next()?row.getString(1):null;}}}
    private static String hash(SaleConsumptionRequest r)throws BasicException{StringBuilder b=new StringBuilder(r.receiptId).append('|').append(r.ticketId).append('|').append(r.locationId);for(SaleLine l:r.lines)b.append('|').append(l.lineNo).append(':').append(l.productId).append(':').append(decimal(l.quantity)).append(':').append(l.service);return sha256(b.toString());}
    private static void validate(SaleConsumptionRequest r)throws BasicException{if(r==null||blank(r.idempotencyKey)||blank(r.receiptId)||r.ticketId<=0||blank(r.locationId)||r.lines==null||blank(r.createdBy))throw new BasicException("Invalid sale recipe consumption request.");}
    private static void validate(RefundRequest r)throws BasicException{if(r==null||blank(r.idempotencyKey)||r.sourceTicketId<=0||blank(r.createdBy))throw new BasicException("Invalid sale recipe reversal request.");}
    private static BigDecimal decimal(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private static BigDecimal qty(BigDecimal v){return decimal(v).setScale(SCALE,RoundingMode.HALF_UP);}
    private static BigDecimal money(BigDecimal v){return decimal(v).setScale(6,RoundingMode.HALF_UP);}
    private static String sha256(String text)throws BasicException{try{byte[] d=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new BasicException("SHA-256 is unavailable.",e);}}
    private static boolean blank(String v){return v==null||v.isBlank();}
}
