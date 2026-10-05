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
package com.mx.kylgis.pos.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author adrianromero
 */
public class LabelIcon extends JLabel implements Icon {
    
    private int iconwidth;
    private int iconheight;
    
    /**
     *
     * @param width
     * @param height
     */
    public LabelIcon(int width, int height) {
        iconwidth = width;
        iconheight = height;
    }

    /**
     *
     * @param mywidth
     * @param myheight
     * @return
     */
    public BufferedImage getImage(int mywidth, int myheight) {
        
        BufferedImage imgtext = new BufferedImage(mywidth, myheight,  BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = imgtext.createGraphics();
        
        setBounds(0, 0, mywidth, myheight);          
        paint(g2d);
        g2d.dispose();
        
        return imgtext;         
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        
        setBounds(0, 0, iconwidth, iconheight);
        g.translate(x, y);
        paint(g);
        g.translate(-x, -y);
    }

    public int getIconWidth() {
        return iconwidth;
    }

    public int getIconHeight() {
        return iconheight;
    }

}
