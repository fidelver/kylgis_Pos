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
package com.mx.kylgis.pos.data.model;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.gui.ListCellRendererBasic;
import com.mx.kylgis.pos.data.loader.ComparatorCreator;
import com.mx.kylgis.pos.data.loader.DataRead;
import com.mx.kylgis.pos.data.loader.DataWrite;
import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.loader.IRenderString;
import com.mx.kylgis.pos.data.loader.PreparedSentence;
import com.mx.kylgis.pos.data.loader.SentenceExec;
import com.mx.kylgis.pos.data.loader.SentenceList;
import com.mx.kylgis.pos.data.loader.SerializerRead;
import com.mx.kylgis.pos.data.loader.SerializerWrite;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.data.user.FilterEditorCreator;
import com.mx.kylgis.pos.data.user.ListProvider;
import com.mx.kylgis.pos.data.user.ListProviderCreator;
import com.mx.kylgis.pos.data.user.SaveProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.ListCellRenderer;

/**
 *
 * @author adrian
 */
public class Row {
    
    private Field[] fields;
    
    /**
     *
     * @param fields
     */
    public Row(Field... fields) {
        this.fields = fields;
    }
    
    /**
     *
     * @return
     */
    public Vectorer getVectorer() {
        return new RowVectorer();
    }
    
    /**
     *
     * @return
     */
    public IRenderString getRenderString() {
        return new RowRenderString();
    }

    /**
     *
     * @return
     */
    public ListCellRenderer getListCellRenderer() {  
        return new ListCellRendererBasic(new RowRenderString());  
    }
    
    /**
     *
     * @return
     */
    public ComparatorCreator getComparatorCreator() {
        return new RowComparatorCreator();
    }

    /**
     *
     * @param s
     * @param sql
     * @param indexes
     * @return
     */
    public SentenceExec getExecSentence(Session s, String sql, final int... indexes) {
        return new PreparedSentence(s, sql, 
            new SerializerWrite<Object[]>() {
                @Override
                public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                    for (int i = 0; i < indexes.length; i++) {
                        fields[indexes[i]].getData().setValue(dp, i + 1, obj[indexes[i]]);
                    }
                }            
            }
        );
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public ListProvider getListProvider(Session s, Table t) {
        return new ListProviderCreator(getListSentence(s, t));        
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public SaveProvider getSaveProvider(Session s, Table t) {
        return new SaveProvider(getUpdateSentence(s, t), getInsertSentence(s, t), getDeleteSentence(s, t));
    }
    
    /**
     *
     * @param s
     * @param sql
     * @param sw
     * @return
     */
    public SentenceList getListSentence(Session s, String sql, SerializerWrite sw) {
        return new PreparedSentence(s, sql, sw, new RowSerializerRead());
    }
    
    /**
     *
     * @param s
     * @param sql
     * @param filter
     * @return
     */
    public ListProvider getListProvider(Session s, String sql, FilterEditorCreator filter) {
        return new ListProviderCreator(getListSentence(s, sql, filter.getSerializerWrite()), filter);
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public SentenceList getListSentence(Session s, Table t) {
        return getListSentence(s, t.getListSQL(), null);
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public SentenceExec getInsertSentence(Session s, final Table t) {
        return new PreparedSentence(s,  t.getInsertSQL(), 
            new SerializerWrite<Object[]>() {
                @Override
                public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                    for (int i = 0; i < t.getColumns().length; i++) {
                        fields[i].getData().setValue(dp, i + 1, obj[i]);
                    }           
                }            
            }
        );
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public SentenceExec getDeleteSentence(Session s, final Table t) {
        return new PreparedSentence(s,  t.getDeleteSQL(), 
            new SerializerWrite<Object[]>() {
                @Override
                public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                    int index = 1;
                    for (int i = 0; i < t.getColumns().length; i++) {
                        if (t.getColumns()[i].isPK()) {
                            fields[i].getData().setValue(dp, index++, obj[i]);
                        }
                    }           
                }            
            }
        );        
    }
    
    /**
     *
     * @param s
     * @param t
     * @return
     */
    public SentenceExec getUpdateSentence(Session s, final Table t) {
        return new PreparedSentence(s,  t.getUpdateSQL(), 
            new SerializerWrite<Object[]>() {
                @Override
                public void writeValues(DataWrite dp, Object[] obj) throws BasicException {
                    int index = 1;
                    for (int i = 0; i < t.getColumns().length; i++) {
                        if (!t.getColumns()[i].isPK()) {
                            fields[i].getData().setValue(dp, index++, obj[i]);
                        }
                    }   
                    for (int i = 0; i < t.getColumns().length; i++) {
                        if (t.getColumns()[i].isPK()) {
                            fields[i].getData().setValue(dp, index++, obj[i]);
                        }
                    }                         
                }            
            }
        );        
    }

    /**
     *
     * @return
     */
    public Datas[] getDatas() {
        Datas[] d = new Datas[fields.length];
        for (int i = 0; i < fields.length; i++) {
            d[i] = fields[i].getData();
        }
        return d;
    }

    /**
     *
     * @return
     */
    public SerializerRead getSerializerRead() {
        return new RowSerializerRead();
    }
    
    private class RowSerializerRead implements SerializerRead {
        @Override
        public Object readValues(DataRead dr) throws BasicException {             
            Object[] m_values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                m_values[i] = fields[i].getData().getValue(dr, i + 1);
            }
            return m_values;
        }
    }  
    
    private class RowVectorer implements Vectorer {
        @Override
        public String[] getHeaders() throws BasicException {
// JG Aug 2013 use Diamon Inference
            List<String> l = new ArrayList<>();
            for (Field f : fields) {
                if (f.isSearchable()) {
                    l.add(f.getLabel());
                }
            }
            return l.toArray(new String[l.size()]);
        }
        @Override
        public String[] getValues(Object obj) throws BasicException {   
            Object[] values = (Object[]) obj;            
// JG Aug 2013 use Diamon Inference
            List<String> l = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isSearchable()) {
                    l.add(fields[i].getFormat().formatValue(values[i]));
                }
            }
            return l.toArray(new String[l.size()]);
        }
    }  
    
    private class RowRenderString implements IRenderString {
        @Override
        public String getRenderString(Object value) {        
            Object[] values = (Object[]) value;            
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isTitle()) {
                    if (s.length() > 0) {
                        s.append(" - ");
                    }
                    s.append(fields[i].getFormat().formatValue(values[i]));
                }
            }
            return s.toString();
        }
    }
    
    private class RowComparatorCreator implements ComparatorCreator {
        
        private List<Integer> comparablefields = new ArrayList<>();
        
        public RowComparatorCreator() {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isComparable()) {
                    comparablefields.add(i);
                }
            }            
        }
        
        @Override
        public String[] getHeaders() {
            String [] headers = new String [comparablefields.size()];
            for (int i = 0; i < comparablefields.size(); i++) {
                headers[i] = fields[comparablefields.get(i)].getLabel();
            }
            return headers;
        }   
        
        @Override
        public Comparator createComparator(final int[] orderby) {
            return new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    if (o1 == null) {
                        if (o2 == null) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else if (o2 == null) {
                        return +1;
                    } else {
                        Object[] ao1 = (Object[]) o1;
                        Object[] ao2 = (Object[]) o2;
                        for (int i = 0; i < orderby.length; i++) {
                            int result = fields[comparablefields.get(orderby[i])].getData().compare(
                                    ao1[comparablefields.get(orderby[i])], 
                                    ao2[comparablefields.get(orderby[i])]);
                            if (result != 0) {
                                return result;
                            }
                        }
                        return 0;
                    }
                }
            };
        }        
    }   
}
