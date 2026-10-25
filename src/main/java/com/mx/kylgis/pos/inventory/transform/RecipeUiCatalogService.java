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
        String sql = "SELECT id, reference, name, uom FROM products "
                + "WHERE uom IS NOT NULL AND TRIM(uom) <> '' ORDER BY name, reference";
        try {
            Connection connection = session.getConnection();
            List<ProductChoice> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new ProductChoice(rows.getString(1), rows.getString(2),
                            rows.getString(3), rows.getString(4)));
                }
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException ex) {
            throw new BasicException("Unable to load recipe product catalog.", ex);
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
        public final String name;
        public final String uomId;

        ProductChoice(String id, String reference, String name, String uomId) {
            this.id = id;
            this.reference = reference;
            this.name = name;
            this.uomId = uomId;
        }

        @Override
        public String toString() {
            String displayName = name == null ? "" : name.trim();
            String displayReference = reference == null ? "" : reference.trim();
            if (displayName.isEmpty()) {
                return displayReference.isEmpty()
                        ? AppLocal.getIntString("recipe.catalog.unknown.product")
                        : displayReference;
            }
            return displayReference.isEmpty() ? displayName : displayReference + " - " + displayName;
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
