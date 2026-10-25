-- Fase 2 SAT / CFDI 4.0 (MariaDB/MySQL). ADDITIVE ONLY.
-- PRECONDITION: backup, schema review, exclusive migration window.
-- NEVER apply automatically to production. No legacy RATE inference.
-- Use legacy utf8 here to match existing taxes/products FK column charset/collation.
-- A future full-table utf8mb4 migration needs separate FK/charset certification.
CREATE TABLE IF NOT EXISTS taxes_sat (
  tax_id VARCHAR(255) NOT NULL,
  impuesto CHAR(3) DEFAULT NULL,
  tipo_factor VARCHAR(8) DEFAULT NULL,
  naturaleza VARCHAR(10) DEFAULT NULL,
  tasa_cuota DECIMAL(18,6) DEFAULT NULL,
  cuota_unidad VARCHAR(32) DEFAULT NULL,
  vigencia_desde DATE DEFAULT NULL,
  vigencia_hasta DATE DEFAULT NULL,
  sat_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (tax_id),
  CONSTRAINT fk_taxes_sat_tax FOREIGN KEY (tax_id) REFERENCES taxes(id) ON DELETE CASCADE,
  CONSTRAINT ck_taxes_sat_rate CHECK (tasa_cuota IS NULL OR tasa_cuota >= 0),
  CONSTRAINT ck_taxes_sat_dates CHECK (vigencia_hasta IS NULL OR vigencia_desde IS NULL OR vigencia_hasta >= vigencia_desde)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS products_sat (
  product_id VARCHAR(255) NOT NULL,
  objeto_imp CHAR(2) DEFAULT NULL,
  clave_prodserv CHAR(8) DEFAULT NULL,
  clave_unidad VARCHAR(3) DEFAULT NULL,
  cuota_cantidad_unidad DECIMAL(18,6) DEFAULT NULL,
  cuota_unidad VARCHAR(32) DEFAULT NULL,
  PRIMARY KEY (product_id),
  CONSTRAINT fk_products_sat_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Snapshot immutability: this table is deliberately NOT backfilled from
-- historical tickets; rows must be written together with newly saved tickets
-- by the future coordinated sale/CFDI integration (same DB transaction).
CREATE TABLE IF NOT EXISTS ticketlines_sat (
  ticket_id VARCHAR(255) NOT NULL,
  line_no INTEGER NOT NULL,
  objeto_imp CHAR(2) DEFAULT NULL,
  clave_prodserv CHAR(8) DEFAULT NULL,
  clave_unidad VARCHAR(3) DEFAULT NULL,
  PRIMARY KEY (ticket_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ticketlines_sat_taxes (
  ticket_id VARCHAR(255) NOT NULL,
  line_no INTEGER NOT NULL,
  tax_id VARCHAR(255) NOT NULL,
  impuesto CHAR(3) NOT NULL,
  tipo_factor VARCHAR(8) NOT NULL,
  naturaleza VARCHAR(10) NOT NULL,
  tasa_cuota DECIMAL(18,6) DEFAULT NULL,
  cuota_unidad VARCHAR(32) DEFAULT NULL,
  base DECIMAL(20,6) NOT NULL,
  cantidad_fiscal DECIMAL(20,6) DEFAULT NULL,
  importe DECIMAL(20,6) NOT NULL,
  PRIMARY KEY (ticket_id, line_no, tax_id),
  CONSTRAINT fk_sat_taxline_concept FOREIGN KEY (ticket_id, line_no)
      REFERENCES ticketlines_sat(ticket_id, line_no) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- All old taxes remain unmapped. Do not UPDATE an existing fiscal database
-- based on 0%, labels, names, or negative legacy percentages.
-- For fresh installations only, explicitly review/enable profiles:
-- 000 IVA Exento (002, Exento, Traslado, NULL)
-- 001 IVA 16% (002, Tasa, Traslado, 0.160000)
-- 002 IVA 0%  (002, Tasa, Traslado, 0.000000)