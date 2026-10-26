-- Fase 2 SAT / CFDI 4.0 (PostgreSQL). Additive, no legacy data inference.
-- Backup and fiscal approval required before execution.
CREATE TABLE IF NOT EXISTS taxes_sat (
  tax_id VARCHAR(255) PRIMARY KEY REFERENCES taxes(id) ON DELETE CASCADE,
  impuesto CHAR(3),
  tipo_factor VARCHAR(8),
  naturaleza VARCHAR(10),
  tasa_cuota NUMERIC(18,6) CHECK (tasa_cuota IS NULL OR tasa_cuota >= 0),
  cuota_unidad VARCHAR(32),
  vigencia_desde DATE,
  vigencia_hasta DATE,
  sat_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT ck_taxes_sat_dates CHECK (vigencia_hasta IS NULL OR vigencia_desde IS NULL OR vigencia_hasta >= vigencia_desde)
);
CREATE TABLE IF NOT EXISTS products_sat (
  product_id VARCHAR(255) PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
  objeto_imp CHAR(2),
  clave_prodserv CHAR(8),
  clave_unidad VARCHAR(3),
  cuota_cantidad_unidad NUMERIC(18,6),
  cuota_unidad VARCHAR(32)
);
CREATE TABLE IF NOT EXISTS ticketlines_sat (
  ticket_id VARCHAR(255) NOT NULL,
  line_no INTEGER NOT NULL,
  objeto_imp CHAR(2),
  clave_prodserv CHAR(8),
  clave_unidad VARCHAR(3),
  PRIMARY KEY (ticket_id, line_no)
);
CREATE TABLE IF NOT EXISTS ticketlines_sat_taxes (
  ticket_id VARCHAR(255) NOT NULL,
  line_no INTEGER NOT NULL,
  tax_id VARCHAR(255) NOT NULL,
  impuesto CHAR(3) NOT NULL,
  tipo_factor VARCHAR(8) NOT NULL,
  naturaleza VARCHAR(10) NOT NULL,
  tasa_cuota NUMERIC(18,6),
  cuota_unidad VARCHAR(32),
  base NUMERIC(20,6) NOT NULL,
  cantidad_fiscal NUMERIC(20,6),
  importe NUMERIC(20,6) NOT NULL,
  PRIMARY KEY (ticket_id,line_no,tax_id),
  FOREIGN KEY (ticket_id,line_no) REFERENCES ticketlines_sat(ticket_id,line_no) ON DELETE CASCADE
);
-- Do not backfill / infer historical VAT zero as Exento or Tasa 0%.