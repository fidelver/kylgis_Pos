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

import com.mx.kylgis.pos.basic.BasicException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author  adrianromero
 */
public class StaticSentence extends JDBCSentence {

    private static final Logger logger = Logger.getLogger("com.mx.kylgis.pos.data.loader.StaticSentence");
    
    private ISQLBuilderStatic m_sentence;

    /**
     *
     */
    protected SerializerWrite m_SerWrite = null;

    /**
     *
     */
    protected SerializerRead m_SerRead = null;

    // Estado
    private Statement m_Stmt;
    
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence
     * @param serread
     * @param serwrite */
    public StaticSentence(Session s, ISQLBuilderStatic sentence, SerializerWrite serwrite, SerializerRead serread) {
        super(s);
        m_sentence = sentence;
        m_SerWrite = serwrite;
        m_SerRead = serread;
        m_Stmt = null;
    }    
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence */
    public StaticSentence(Session s, ISQLBuilderStatic sentence) {
        this(s, sentence, null, null);
    }     
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence
     * @param serwrite */
    public StaticSentence(Session s, ISQLBuilderStatic sentence, SerializerWrite serwrite) {
        this(s, sentence, serwrite, null);
    }     
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence
     * @param serread
     * @param serwrite */
    public StaticSentence(Session s, String sentence, SerializerWrite serwrite, SerializerRead serread) {
        this(s, new NormalBuilder(sentence), serwrite, serread);
    }
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence
     * @param serwrite */
    public StaticSentence(Session s, String sentence, SerializerWrite serwrite) {
        this(s, new NormalBuilder(sentence), serwrite, null);
    }
    /** Creates a new instance of StaticSentence
     * @param s
     * @param sentence */
    public StaticSentence(Session s, String sentence) {
        this(s, new NormalBuilder(sentence), null, null);
    }
    
    /**
     *
     * @param params
     * @return
     * @throws BasicException
     */
    @Override
    public DataResultSet openExec(Object params) throws BasicException {
        // true -> un resultset
        // false -> un updatecount (si -1 entonces se acabo)
        
        closeExec();
            
        try {
            m_Stmt = m_s.getConnection().createStatement();

            String sentence = m_sentence.getSQL(m_SerWrite, params);
            
           logger.log(Level.INFO, "Executing static SQL: {0}", sentence);

            if (m_Stmt.execute(sentence)) {
                return new JDBCDataResultSet(m_Stmt.getResultSet(), m_SerRead);
            } else { 
                int iUC = m_Stmt.getUpdateCount();
                if (iUC < 0) {
                    return null;
                } else {
                    return new SentenceUpdateResultSet(iUC);
                }
            }
        } catch (SQLException eSQL) {
            throw new BasicException(eSQL);
        }
    }
    
    /**
     *
     * @throws BasicException
     */
    @Override
    public void closeExec() throws BasicException {
        
        if (m_Stmt != null) {
            try {
                m_Stmt.close();
           } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            } finally {
                m_Stmt = null;
            }
        }
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    @Override
    public DataResultSet moreResults() throws BasicException {

        try {
            if (m_Stmt.getMoreResults()){
                // tenemos resultset
                return new JDBCDataResultSet(m_Stmt.getResultSet(), m_SerRead);
            } else {
                // tenemos updatecount o si devuelve -1 ya no hay mas
                int iUC = m_Stmt.getUpdateCount();
                if (iUC < 0) {
                    return null;
                } else {
                    return new SentenceUpdateResultSet(iUC);
                }
            }
        } catch (SQLException eSQL) {
            throw new BasicException(eSQL);
        }
    }    
    
}
