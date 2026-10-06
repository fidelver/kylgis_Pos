//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
package com.mx.kylgis.pos.printer.escpos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author JG uniCenta
 */
public class CodesEpson extends Codes {
    
    private static final byte[] INITSEQUENCE = {};

    private static final byte[] CHAR_SIZE_0 = {0x1D, 0x21, 0x00};
    private static final byte[] CHAR_SIZE_1 = {0x1D, 0x21, 0x01};
    private static final byte[] CHAR_SIZE_2 = {0x1D, 0x21, 0x30};
    private static final byte[] CHAR_SIZE_3 = {0x1D, 0x21, 0x31};

    public static final byte[] BOLD_SET = {0x1B, 0x45, 0x01};
    public static final byte[] BOLD_RESET = {0x1B, 0x45, 0x00};

    public static final byte[] UNDERLINE_SET = {0x1B, 0x2D, 0x01};
    public static final byte[] UNDERLINE_RESET = {0x1B, 0x2D, 0x00};
    
    private static final byte[] OPEN_DRAWER = {0x1B, 0x70, 0x00, 0x32, -0x06};    
    private static final byte[] PARTIAL_CUT_1 = {0x1B, 0x69};
    // GS v 0: modo 0 = tamaño normal. El modo 3 duplica ancho y alto;
    // en papel de 384 dots eso convertía el raster en 768 dots y recortaba
    // la mitad derecha, haciendo que un logo matemáticamente centrado pareciera
    // desplazado. El escalado necesario se hace antes de generar el raster.
    private static final byte[] IMAGE_HEADER = {0x1D, 0x76, 0x30, 0x00};
    private static final byte[] NEW_LINE = {0x0D, 0x0A}; // Print and carriage return
    private static final byte[] IMAGE_LOGO = {0x1B, 0x1C, 0x70, 0x01, 0x00};
    
    /** Creates a new instance of CodesEpson */
    public CodesEpson() {
    }

    /**
     *
     * @return
     */
    @Override
    public byte[] getInitSequence() { return INITSEQUENCE; }
     
    /**
     *
     * @return
     */
    @Override
    public byte[] getSize0() { return CHAR_SIZE_0; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getSize1() { return CHAR_SIZE_1; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getSize2() { return CHAR_SIZE_2; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getSize3() { return CHAR_SIZE_3; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getBoldSet() { return BOLD_SET; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getBoldReset() { return BOLD_RESET; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getUnderlineSet() { return UNDERLINE_SET; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getUnderlineReset() { return UNDERLINE_RESET; }
    
    /**
     *
     * @return
     */
    @Override
    public byte[] getOpenDrawer() { return OPEN_DRAWER; }   

    /**
     *
     * @return
     */
    @Override
    public byte[] getCutReceipt() { return PARTIAL_CUT_1; }    

    /**
     *
     * @return
     */
    @Override
    public byte[] getNewLine() { return NEW_LINE; } 

    /**
     *
     * @return
     */
    @Override
    public byte[] getImageHeader() { return IMAGE_HEADER; }

    /**
     * GS v 0 se envia en modo normal para no duplicar tambien el lienzo de
     * 384 dots. Escalamos solo la imagen visible al doble antes de rasterizarla,
     * conservando el centrado real y el tamano visual que tenia el modo 3.
     */
    @Override
    public byte[] transImage(BufferedImage image) {
        int maxWidth = getRasterImageWidth();
        int targetWidth = Math.min(maxWidth, image.getWidth() * 2);
        int targetHeight = Math.max(1, (int) Math.round(
                image.getHeight() * (targetWidth / (double) image.getWidth())));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return super.transImage(scaled);
    }

    /**
     *
     * @return
     */
    @Override
    public int getImageWidth() { return 256; }

    /**
     *
     * @return
     */
    @Override
    public byte[] getImageLogo(){ return IMAGE_LOGO; }
}
