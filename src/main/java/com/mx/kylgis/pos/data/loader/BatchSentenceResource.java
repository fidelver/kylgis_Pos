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
package com.mx.kylgis.pos.data.loader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import com.mx.kylgis.pos.basic.BasicException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author adrianromero
 */
public class BatchSentenceResource extends BatchSentence {

    private String m_sResScript;
    
    /** Creates a new instance of BatchSentenceResource
     * @param s
     * @param resscript */
    public BatchSentenceResource(Session s, String resscript) {
        super(s);
        m_sResScript = resscript;
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    protected Reader getReader() throws BasicException {
        
        InputStream in = BatchSentenceResource.class.getResourceAsStream(m_sResScript);
        
        if (in == null) {
            throw new BasicException(LocalRes.getIntString("exception.nosentencesfile"));
        } else {  
            try {
                return new InputStreamReader(in, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new BasicException(LocalRes.getIntString("exception.nosentencesfile"), ex);
            }
        }
    }   
}
