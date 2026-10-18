#!/usr/bin/env python3
"""Casos de regresión del linter; no sustituyen ejecución SQL sobre LAB."""
import unittest
import audit_report_contracts as report


class ReportContractTests(unittest.TestCase):
    def test_sql_commas_with_functions_and_literals(self):
        query = "SELECT COALESCE(x, 0) AS UNITS, CONCAT('a,b', c) AS CODE, t.id AS ID FROM t"
        self.assertEqual([report.select_name(v) for v in report.top_level_select(query)],
                         ["UNITS", "CODE", "ID"])

    def test_queries_with_subselects(self):
        query = ("SELECT a, IFNULL((SELECT b FROM inner_table WHERE id = 1), 0) AS B, "
                 "c FROM outer_table")
        self.assertEqual(len(report.top_level_select(query)), 3)

    def test_literal_fragment_extraction(self):
        sql = report.query_source('report.setSentence("SELECT " + "a, b " + "FROM products");')
        self.assertEqual(sql, "SELECT a, b FROM products")

    def test_debtor_contract_now_matches(self):
        result = report.audit_file(report.REPORTS / "customers_b.bs")
        self.assertNotIn("missing_jasper_fields", result["errors"])
        self.assertFalse(result["missing_fields"])

    def test_five_inherited_failures_detected(self):
        # La prueba debe detectar defectos pendientes: no confundir detección
        # de un fallo con certificación funcional.
        records = {x["name"]: x for x in report.audit()}
        expected = {
            "products_catalog", "sales_categorysalesmysql",
            "sales_extproducts_1", "sales_productsalesprofit_1", "vouchers",
        }
        failed = {n for n, r in records.items() if r["status"] == "FAIL"}
        self.assertTrue(failed.issubset(expected), f"Nuevas regresiones: {failed - expected}")
        self.assertEqual(len(records), 69)
        self.assertEqual(len(list(report.REPORTS.glob("*.jrxml"))), 67)

    def test_profit_cartesian_guard(self):
        result = report.audit_file(report.REPORTS / "sales_productsalesprofit_1.bs")
        self.assertIn("possible_cartesian_products_ticketlines", result["errors"])

    def test_voucher_filters_guard(self):
        result = report.audit_file(report.REPORTS / "vouchers.bs")
        self.assertIn("voucher_filters_not_applied", result["errors"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
