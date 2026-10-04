package com.openbravo.format;

public class ImporteALetras {

    private static final String[] _grupos =
        { "", "millon", "billon", "trillon" };

    private static final String[] _unidades =
        { "", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve" };

    private static final String[] _decena1 =
        { "", "once", "doce", "trece", "catorce", "quince", "dieciseis",
          "diecisiete", "dieciocho", "diecinueve" };

    private static final String[] _decenas =
        { "", "diez", "veinte", "treinta", "cuarenta", "cincuenta",
          "sesenta", "setenta", "ochenta", "noventa" };

    private static final String[] _centenas =
        { "", "cien", "doscientos", "trescientos", "cuatrocientos",
          "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos" };

    public static String millarATexto(int n) {
        if (n == 0) return "";

        int centenas = n / 100;
        n = n % 100;
        int decenas = n / 10;
        int unidades = n % 10;

        String sufijo = "";

        if (decenas == 0 && unidades != 0)
            sufijo = _unidades[unidades];

        if (decenas == 1 && unidades != 0)
            sufijo = _decena1[unidades];

        if (decenas == 2 && unidades != 0)
            sufijo = "veinti" + _unidades[unidades];

        if (unidades == 0)
            sufijo = _decenas[decenas];

        if (decenas > 2 && unidades != 0)
            sufijo = _decenas[decenas] + " y " + _unidades[unidades];

        if (centenas != 1)
            return _centenas[centenas] + " " + sufijo;

        if (unidades == 0 && decenas == 0)
            return "cien";

        return "ciento " + sufijo;
    }

    public static String numeroACastellano(long n) {
        String resultado = "";
        int grupo = 0;
        while (n != 0 && grupo < _grupos.length) {
            long fragmento = n % 1000000;
            int millarAlto = (int) (fragmento / 1000);
            int millarBajo = (int) (fragmento % 1000);
            n = n / 1000000;

            String nombreGrupo = _grupos[grupo];
            if (fragmento > 1 && grupo > 0)
                nombreGrupo += "es";

            if ((millarAlto != 0) || (millarBajo != 0)) {
                if (millarAlto > 1)
                    resultado = millarATexto(millarAlto) + " mil " +
                                millarATexto(millarBajo) + " " +
                                nombreGrupo + " " + resultado;

                if (millarAlto == 0)
                    resultado = millarATexto(millarBajo) + " " +
                                nombreGrupo + " " + resultado;

                if (millarAlto == 1)
                    resultado = "mil " + millarATexto(millarBajo) + " " +
                                nombreGrupo + " " + resultado;
            }
            grupo++;
        }
        return resultado;
    }

    /**
     * Formatea el importe en letras, con la primera letra alfabética en mayúscula.
     * Siempre devuelve el valor absoluto, sin signo y sin la palabra "menos",
     * porque la naturaleza del documento (venta, devolución, nota de crédito)
     * ya indica la dirección del flujo. El importe en letra representa el
     * valor nominal del comprobante, no su signo contable.
     */
    public static String formatear(Double total) {
        if (total == null) total = 0.0;

        // Valor absoluto: sin importar el signo del total, siempre positivo
        double numero = Math.abs(total);
        String sing = "(";

        numero = Math.round(numero * 100.0) / 100.0;
        long numero_entero = (long) Math.floor(numero);
        long centavos = Math.round((numero - numero_entero) * 100);

        String value;
        if (centavos == 0) {
            value = sing + numeroACastellano(numero_entero) + "pesos 00/100 M.N.)";
        } else if (numero_entero < 1) {
            value = sing + Long.toString(centavos) + "/100 M.N.)";
        } else {
            value = sing + numeroACastellano(numero_entero) + " pesos "
                    + Long.toString(centavos) + "/100 M.N.)";
        }

        // Convertir a mayúscula la primera letra alfabética
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetter(c)) {
                value = value.substring(0, i)
                      + Character.toUpperCase(c)
                      + value.substring(i + 1);
                break;
            }
        }

        return value;
    }
    /**
     * Devuelve el importe en letra dividido en líneas, tratando la coletilla
     * "NN/100 M.N.)" como una unidad indivisible. Recibe el total ya calculado
     * (útil para la previsualización, donde el ticket aún no tiene total).
     *
     * @param total monto a convertir
     * @param ancho número máximo de caracteres por línea
     * @return lista de líneas listas para imprimir
     */
    public static java.util.List<String> formatearLineas(double total, int ancho) {
        String s = formatear(total);

        java.util.List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+/100 M\\.N\\.\\)|\\S+)")
                .matcher(s);
        while (m.find()) {
            tokens.add(m.group());
        }

        java.util.List<String> lineas = new java.util.ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String token : tokens) {
            if (actual.length() == 0) {
                actual.append(token);
            } else if (actual.length() + 1 + token.length() <= ancho) {
                actual.append(' ').append(token);
            } else {
                lineas.add(actual.toString());
                actual = new StringBuilder(token);
            }
        }
        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }
    
 }