#!/usr/bin/env python3
"""Closed, offline report QA. Never connects to a database or printer."""
import re
import unittest
import xml.etree.ElementTree as ET

import audit_report_contracts as audit

R = audit.REPORTS

def sql(name):
    return audit.query_source((R / (name + ".bs")).read_text(encoding="utf-8"))

def root(name):
    return ET.parse(R / (name + ".jrxml")).getroot()

def field_variables(name):
    result = {}
    for item in root(name).iter():
        if item.tag.rsplit("}", 1)[-1] != "variable":
            continue
        for child in item:
            if child.tag.rsplit("}", 1)[-1] == "variableExpression":
                result[item.get("name")] = child.text or ""
    return result

class StrictReportQuality(unittest.TestCase):
    def test_seventy_minus_one_scripts_have_full_contracts(self):
        records = audit.audit()
        self.assertEqual(len(records), 69)
        self.assertFalse([r for r in records if r["status"] == "FAIL"])

    def test_repaired_legacy_contracts_have_sql_and_correct_order(self):
        for name in ("customers_b", "products_catalog", "sales_categorysalesmysql",
                     "sales_extproducts_1", "sales_productsalesprofit_1", "vouchers"):
            with self.subTest(report=name):
                state = audit.audit_file(R / (name + ".bs"))
                self.assertFalse(state["errors"], state)
                self.assertEqual(state["sql_columns"], state["mapped_columns"], state)
                self.assertFalse(
                    [w for w in state["warnings"] if w.startswith("sql_field_ordinal")],
                    state)

    def test_products_catalog_reads_stockcurrent_not_nonexistent_products_units(self):
        statement = sql("products_catalog").upper()
        self.assertNotIn("PRODUCTS.UNITS", statement)
        self.assertIn("STOCKCURRENT", statement)
        self.assertIn("AS UNITS", statement)

    def test_voucher_filters_and_grouping(self):
        statement = sql("vouchers").upper()
        self.assertIn("?(QBF_FILTER)", statement)
        self.assertIn("GROUP BY PAYMENTS.VOUCHER", statement)
        self.assertIn("TICKETS.TICKETID", statement)
        self.assertNotIn("TICKETID PAYMENTS.VOUCHER", statement)

    def test_profit_alternatives_not_cartesian(self):
        for report in ("sales_productsalesprofit", "sales_productsalesprofit_1"):
            statement = sql(report).upper()
            self.assertNotRegex(statement, r"FROM\\s+PRODUCTS\\s*,\\s*TICKETLINES")
            self.assertIn("TICKETLINES.PRODUCT = PRODUCTS.ID", statement)
            self.assertIn("GROUP BY", statement)

    def test_profit_is_labelled_estimated(self):
        statements = "".join(root("sales_productsalesprofit").itertext()).upper()
        self.assertIn("ESTIMADO", statements)
        self.assertIn("COSTO DE COMPRA VIGENTE", statements)

    def test_net_and_gross_rollups_do_not_sum_units(self):
        variables = field_variables("sales_extproducts")
        for prefix, actual_field in (("TOTALnet", "TOTALnet"), ("TOTALgross", "TOTALgross")):
            for suffix in ("CAT", "CUST", "SUM"):
                name = prefix + "_" + suffix
                with self.subTest(variable=name):
                    self.assertEqual(variables[name], "$F{" + actual_field + "}")

    def test_report_date_context_visible_in_pdf_templates(self):
        for report in ("sales_closedpos_1", "sales_extproducts"):
            xml = root(report)
            expressions = "\n".join(
                (node.text or "") for node in xml.iter()
                if node.tag.rsplit("}", 1)[-1].endswith("Expression"))
            self.assertIn("$P{KYLGIS_REPORT_PERIOD}", expressions, report)

    def test_debt_is_not_constrained_to_today(self):
        for report in ("customers_debtors", "suppliers_creditors"):
            txt = (R / (report + ".bs")).read_text()
            self.assertNotIn("getToday()", txt)
            self.assertNotIn('addParameter("CURDATE")', txt)
            self.assertIn("COALESCE(CURDEBT, 0) <> 0", txt)

    def test_inventory_diary_includes_hidden_stock_items(self):
        # Sales-catalog visibility must not hide stock movements or ingredients.
        statement = sql("inventory_diary").upper()
        self.assertNotIn("PRODUCTS.VISIBLE", statement)
        self.assertIn("FROM ((STOCKDIARY STOCKDIARY", statement)
        self.assertIn("LEFT OUTER JOIN SUPPLIERS", statement)
        self.assertIn("?(QBF_FILTER)", statement)
        self.assertIn("SUM(STOCKDIARY.UNITS * STOCKDIARY.PRICE)", statement)

    def test_product_sales_chart_fields_and_viewer_diagnostics(self):
        statement = sql("sales_chart_productsales").upper()
        self.assertIn("GROUP BY PRODUCTS.REFERENCE, PRODUCTS.NAME", statement)
        self.assertIn("SUM(TICKETLINES.UNITS) AS UNITS", statement)
        self.assertIn("?(QBF_FILTER)", statement)
        chart_xml = root("sales_chart_productsales")
        expressions = " ".join(
            (node.text or "") for node in chart_xml.iter()
            if node.tag.rsplit("}", 1)[-1].endswith("Expression"))
        self.assertIn("$F{NAME}", expressions)
        self.assertIn("$F{UNITS}", expressions)
        viewer = audit.ROOT / "src/main/java/com/mx/kylgis/pos/reports/JPanelReport.java"
        # The portable QA script is rooted at src/main/resources/...;
        # Java source lives in src/main/java/...
        self.assertTrue(viewer.is_file())
        source = viewer.read_text(encoding="utf-8")
        self.assertIn("KYL-JASPER fill_completed", source)
        self.assertIn("rowsRead == 0 || pages == 0", source)

    def test_sql_source_present_for_all_scripts(self):
        for name in (file.stem for file in R.glob("*.bs")):
            self.assertTrue(sql(name).lstrip().upper().startswith("SELECT"), name)

if __name__ == "__main__":
    unittest.main(verbosity=2)
