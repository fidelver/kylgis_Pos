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
package com.mx.kylgis.pos.data.gui;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.data.user.Finder;
import java.util.regex.*;

/**
 *
 * @author JG uniCenta
 */
public class FindInfo implements Finder {
    
    /**
     *
     */
    public static final int MATCH_STARTFIELD = 0;
    public static final int MATCH_WHOLEFIELD = 1;
    public static final int MATCH_ANYPARTFIELD = 2;
    public static final int MATCH_REGEXP = 3;
    
    private String m_sTextCompare;
    private Pattern m_TextPattern;
    
    private String m_sText; // Texto a buscar
    private int m_iField;   // Campo de busqueda
    private int m_iMatch;   // Tipo de busqueda
    private boolean m_bMatchCase; // Mayusculas / Minusculas
    
    private Vectorer m_vec;
    
    /** Creates a new instance of FindInfo
     * @param vec
     * @param sText
     * @param iField
     * @param iMatch
     * @param bMatchCase */
    public FindInfo(Vectorer vec, String sText, int iField, boolean bMatchCase, int iMatch) {
        m_vec = vec;
        m_sText = sText;
        m_iField = iField;
        m_bMatchCase = bMatchCase;
        m_iMatch = iMatch;
        
        if (iMatch == MATCH_REGEXP) {          
            m_TextPattern = m_bMatchCase 
                ? Pattern.compile(m_sText) 
                : Pattern.compile(m_sText, Pattern.CASE_INSENSITIVE);
        } else {
            m_sTextCompare = m_bMatchCase
                ? m_sText
                : m_sText.toUpperCase();
        }
    }
    
    /** Creates a new instance of FindInfo
     * @param vec */
    public FindInfo(Vectorer vec) {
        this(vec,  "", 0, true, MATCH_ANYPARTFIELD);
    }
    
    /**
     *
     * @return
     */
    public Vectorer getVectorer() {
        return m_vec;
    }

    /**
     *
     * @return
     */
    public String getText() {
        return m_sText;
    }

    /**
     *
     * @return
     */
    public int getField() {
        return m_iField;
    }

    /**
     *
     * @return
     */
    public boolean isMatchCase() {
        return m_bMatchCase;
    }

    /**
     *
     * @return
     */
    public int getMatch() {
        return m_iMatch;
    }
   
    /**
     *
     * @param obj
     * @return
     * @throws BasicException
     */
    public boolean match(Object obj) throws BasicException {
        
        String[] v = m_vec.getValues(obj);
        
        String sField = m_bMatchCase
            ? v[m_iField]
            : v[m_iField].toUpperCase();
        
        switch (m_iMatch) {
        case MATCH_STARTFIELD:
            return sField.startsWith(m_sTextCompare);
        case MATCH_WHOLEFIELD:
            return sField.equals(m_sTextCompare);
        case MATCH_ANYPARTFIELD:   
            return sField.contains(m_sTextCompare);
        case MATCH_REGEXP:
            return m_TextPattern.matcher(sField).matches();
        default:
            return false;
        }       
    }
    
}
