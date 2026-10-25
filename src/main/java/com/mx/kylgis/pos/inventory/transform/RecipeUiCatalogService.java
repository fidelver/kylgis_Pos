//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.inventory.transform;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppLocal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lightweight read-only catalogs used by recipe/transform UI. */
public final class RecipeUiCatalogService {

    private final Session session;

    public RecipeUiCatalogService(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    public List<ProductChoice> listProducts() throws BasicException {
        String sql = "SELECT p.id, p.reference, p.code, p.name, p.uom, c.name "
                + "FROM products p LEFT JOIN categories c ON c.id = p.category "
                + "WHERE p.uom IS NOT NULL AND TRIM(p.uom) <> '' "
                + "ORDER BY p.name, p.reference, p.code";
        try {
            Connection connection = session.getConnection();
            List<ProductChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new ProductChoice(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4),
                            rows.getString(5), rows.getString(6)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load recipe product catalog.", ex);
        }
    }

    /** Products that can actually be produced: one active PRODUCTION recipe. */
    public List<ProductChoice> listProductionProducts() throws BasicException {
        String sql = "SELECT p.id, p.reference, p.code, p.name, p.uom, c.name "
                + "FROM products p LEFT JOIN categories c ON c.id = p.category "
                + "WHERE p.uom IS NOT NULL AND TRIM(p.uom) <> '' "
                + "AND EXISTS (SELECT 1 FROM recipe_versions rv "
                + "WHERE rv.product_id = p.id AND UPPER(rv.status) = 'ACTIVE' "
                + "AND UPPER(rv.recipe_mode) = 'PRODUCTION') "
                + "ORDER BY p.name, p.reference, p.code";
        try {
            Connection connection = session.getConnection();
            List<ProductChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new ProductChoice(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4),
                            rows.getString(5), rows.getString(6)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load production recipe product catalog.", ex);
        }
    }

    public List<ProductChoice> listRecipeIngredients() throws BasicException {
        String sql = "SELECT p.id, p.reference, p.code, p.name, p.uom, c.name "
                + "FROM products p LEFT JOIN categories c ON c.id = p.category "
                + "WHERE p.uom IS NOT NULL AND TRIM(p.uom) <> '' "
                + "AND p.ISRECIPEINGREDIENT = TRUE "
                + "ORDER BY p.name, p.reference, p.code";
        try {
            Connection connection = session.getConnection();
            List<ProductChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new ProductChoice(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4),
                            rows.getString(5), rows.getString(6)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load recipe ingredient catalog.", ex);
        }
    }

    public List<UomChoice> listUoms() throws BasicException {
        try {
            Connection connection = session.getConnection();
            List<UomChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM uom ORDER BY name, id");
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new UomChoice(rows.getString(1), rows.getString(2)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load UOM catalog.", ex);
        }
    }

    public List<LocationChoice> listLocations() throws BasicException {
        try {
            Connection connection = session.getConnection();
            List<LocationChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name FROM locations ORDER BY name, id");
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new LocationChoice(rows.getString(1), rows.getString(2)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load location catalog.", ex);
        }
    }

    public static final class ProductChoice {
        public final String id;
        public final String reference;
        public final String code;
        public final String name;
        public final String uomId;
        public final String categoryName;

        ProductChoice(String id, String reference, String code, String name,
                String uomId, String categoryName) {
            this.id = id;
            this.reference = reference;
            this.code = code;
            this.name = name;
            this.uomId = uomId;
            this.categoryName = categoryName;
        }

        @Override
        public String toString() {
            String displayName = name == null ? "" : name.trim();
            String displayReference = reference == null ? "" : reference.trim();
            String displayCode = code == null ? "" : code.trim();
            String displayIdentifier = displayCode.isEmpty() ? displayReference : displayCode;
            if (!displayReference.isEmpty() && !displayCode.isEmpty()
                    && !displayReference.equalsIgnoreCase(displayCode)) {
                displayIdentifier = displayReference + " / " + displayCode;
            }
            if (displayName.isEmpty()) {
                return displayIdentifier.isEmpty()
                        ? AppLocal.getIntString("recipe.catalog.unknown.product")
                        : displayIdentifier;
            }
            String displayCategory = categoryName == null ? "" : categoryName.trim();
            String label = displayIdentifier.isEmpty() ? displayName
                    : displayName + " [" + displayIdentifier + "]";
            return displayCategory.isEmpty() ? label : label + " — " + displayCategory;
        }

        boolean matchesSearch(String query) {
            if (query == null || query.trim().isEmpty()) {
                return true;
            }
            String normalized = query.trim().toLowerCase(java.util.Locale.ROOT);
            String searchable = String.join(" ", safe(reference), safe(code), safe(name), safe(categoryName))
                    .toLowerCase(java.util.Locale.ROOT);
            for (String token : normalized.split("\\s+")) {
                if (!searchable.contains(token)) {
                    return false;
                }
            }
            return true;
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public static final class UomChoice {
        public final String id;
        public final String name;

        UomChoice(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            String displayName = name == null ? "" : name.trim();
            return displayName.isEmpty()
                    ? AppLocal.getIntString("recipe.catalog.unknown.uom")
                    : displayName;
        }
    }

    public static final class LocationChoice {
        public final String id;
        public final String name;

        LocationChoice(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            String displayName = name == null ? "" : name.trim();
            return displayName.isEmpty()
                    ? AppLocal.getIntString("recipe.catalog.unknown.location")
                    : displayName;
        }
    }
}
