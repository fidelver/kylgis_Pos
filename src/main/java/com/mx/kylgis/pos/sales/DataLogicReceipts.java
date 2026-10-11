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
package com.mx.kylgis.pos.sales;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.loader.PreparedSentence;
import com.mx.kylgis.pos.data.loader.SerializerReadBasic;
import com.mx.kylgis.pos.data.loader.SerializerReadClass;
import com.mx.kylgis.pos.data.loader.SerializerWriteBasicExt;
import com.mx.kylgis.pos.data.loader.SerializerWriteString;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.data.loader.StaticSentence;
import com.mx.kylgis.pos.forms.BeanFactoryDataSingle;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.util.List;

/**
 *
 * @author adrianromero
 */
public class DataLogicReceipts extends BeanFactoryDataSingle {
    
    private Session s;
    
    /** Creates a new instance of DataLogicReceipts */
    public DataLogicReceipts() {
    }
    
    /**
     *
     * @param s
     */
    @Override
    public void init(Session s){
        this.s = s;
    }
     
    /**
     *
     * @param Id
     * @return
     * @throws BasicException
     */
    public final TicketInfo getSharedTicket(String Id) throws BasicException {
        
        if (Id == null) {
            return null; 
        } else {
            Object[]record = (Object[]) new StaticSentence(s
                    , "SELECT CONTENT, LOCKED FROM sharedtickets WHERE ID = ?"
                    , SerializerWriteString.INSTANCE
                    , new SerializerReadBasic(new Datas[] {
                        Datas.SERIALIZABLE})).find(Id);
            return record == null ? null : (TicketInfo) record[0];
        }
    }
    
        /**
     *
     * @param pickupId
     * @return
     * @throws BasicException
     */
    public final TicketInfo getSharedTicketPickupId(String pickupId) throws BasicException {
        
        if (pickupId == null) {
            return null; 
        } else {
            Object[]record = (Object[]) new StaticSentence(s
                    , "SELECT CONTENT, LOCKED FROM sharedtickets WHERE PICKUPID = ?"
                    , SerializerWriteString.INSTANCE
                    , new SerializerReadBasic(new Datas[] {
                        Datas.SERIALIZABLE})).find(pickupId);
            return record == null ? null : (TicketInfo) record[0];
        }
    } 

    /**
     * JG Dec 14 Administrator and Manager Roles always have access to ALL SHAREDtickets
     * @return
     * @throws BasicException
     */
    public final List<SharedTicketInfo> getSharedTicketList() throws BasicException {
        
        return (List<SharedTicketInfo>) new StaticSentence(s
//                , "SELECT ID, NAME, CONTENT, APPUSER, PICKUPID, LOCKED FROM sharedtickets ORDER BY ID"
                , "SELECT ID, NAME, CONTENT, APPUSER, PICKUPID, LOCKED "
                        + "FROM sharedtickets ORDER BY PICKUPID" 
                , null
                , new SerializerReadClass(SharedTicketInfo.class)).list();
    }
    
    /**
     * Return only current APPUSER SHAREDtickets
     * @param appuser
     * @return
     * @throws BasicException
     */
    public final List<SharedTicketInfo> getUserSharedTicketList(String appuser) throws BasicException {
        String sql = "SELECT ID, NAME, CONTENT, APPUSER, PICKUPID, LOCKED " 
                + "FROM sharedtickets "
                + "WHERE APPUSER =\""+ appuser +"\" ORDER BY PICKUPID";
        
            return (List<SharedTicketInfo>) new StaticSentence(s
            , sql
            , null
            , new SerializerReadClass(SharedTicketInfo.class)).list();
    }
    
    /**
     * For Standard View
     * @param id
     * @param ticket
     * @param pickupid
     * @throws BasicException
     */
    public final void insertSharedTicket(final String id, final TicketInfo ticket, int pickupid) throws BasicException {
        
        Object[] values = new Object[] {
            id, 
            ticket.getName(), 
            ticket,
            ticket.getUser().getId(),
            pickupid

        };
        Datas[] datas;
        datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING, 
            Datas.SERIALIZABLE, 
            Datas.STRING,            
            Datas.INT

        };
        new PreparedSentence(s
            , "INSERT INTO sharedtickets ("
                + "ID, "
                + "NAME, "
                + "CONTENT, "
                + "APPUSER, "                    
                + "PICKUPID) "
                + "VALUES (?, ?, ?, ?, ?)"                
            , new SerializerWriteBasicExt(datas, new int[] {0, 1, 2, 3, 4})).exec(values);                
    }
    
    /**
     *
     * @param id
     * @param ticket
     * @param pickupid
     * @throws BasicException
     */
    public final void updateSharedTicket(final String id, final TicketInfo ticket, int pickupid) throws BasicException {
            
        Object[] values = new Object[] {
            id, 
            ticket.getName(), 
            ticket, 
            ticket.getUser().getId(),
            pickupid
        };
        Datas[] datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING, 
            Datas.SERIALIZABLE, 
            Datas.STRING,
            Datas.INT
        };
        new PreparedSentence(s
                , "UPDATE sharedtickets SET "
                + "NAME = ?, "
                + "CONTENT = ?, "
                + "APPUSER = ?, "                        
                + "PICKUPID = ? "
                + "WHERE ID = ?"
                , new SerializerWriteBasicExt(datas, 
                        new int[] {
                            1, 2, 3, 4, 0})).exec(values);
    }

    /**
     *
     * @param id
     * @param ticket
     * @param pickupid
     * @throws BasicException
     */
    public final void updateRSharedTicket(
            final String id, 
            final TicketInfo ticket, 
            int pickupid) throws BasicException {
            
        Object[] values = new Object[] {
            id, 
            ticket.getName(), 
            ticket, 
            pickupid
        };
        Datas[] datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING, 
            Datas.SERIALIZABLE, 
            Datas.INT
        };
        new PreparedSentence(s
                , "UPDATE sharedtickets SET "
                + "NAME = ?, "
                + "CONTENT = ?, "
                + "PICKUPID = ? "
                + "WHERE ID = ?"
                , new SerializerWriteBasicExt(datas, new int[] {1, 2, 3, 0})).exec(values);                
    }
    
     /**
     * In place for multi-purposing like containing data from elsewhere and/or
     * using Place and User for Notifications
     * @param id
     * @param locked
     * @throws BasicException
     */
    public final void lockSharedTicket(final String id, final String locked) throws BasicException {
            
        Object[] values = new Object[] {
            id, 
            locked
        };
        Datas[] datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING
        };
        new PreparedSentence(s
                , "UPDATE sharedtickets SET "
                + "LOCKED = ? "
                + "WHERE ID = ?"
                , new SerializerWriteBasicExt(datas, new int[] {1, 0})).exec(values);
    }

     /**
     * In place for multi-purposing like flushing locks from elsewhere and/or
     * using Place and User for Notifications
     * @param id
     * @param unlocked
     * @throws BasicException
     */
    public final void unlockSharedTicket(final String id, final String unlocked) throws BasicException {
            
        Object[] values = new Object[] {
            id, 
            unlocked
        };
        Datas[] datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING
        };
        new PreparedSentence(s
                , "UPDATE sharedtickets SET "
                + "LOCKED = ? "
                + "WHERE ID = ?"
                , new SerializerWriteBasicExt(datas, new int[] {1, 0})).exec(values);
    }    

    /** Atomically acquires a shared-ticket lock for one POS instance. */
    public final boolean tryLockSharedTicket(final String id, final String owner) throws BasicException {
        Object[] values = new Object[] {id, owner};
        Datas[] datas = new Datas[] {Datas.STRING, Datas.STRING};
        int updated = new PreparedSentence(s,
                "UPDATE sharedtickets SET LOCKED = ? "
                + "WHERE ID = ? AND (LOCKED IS NULL OR LOCKED = '' OR LOCKED = ?)",
                new SerializerWriteBasicExt(datas, new int[] {1, 0, 1})).exec(values);
        return updated == 1;
    }

    /** Clears a shared-ticket lock only if the requester still owns it. */
    public final boolean unlockSharedTicketIfOwned(final String id, final String owner) throws BasicException {
        Object[] values = new Object[] {id, owner};
        Datas[] datas = new Datas[] {Datas.STRING, Datas.STRING};
        int updated = new PreparedSentence(s,
                "UPDATE sharedtickets SET LOCKED = NULL WHERE ID = ? AND LOCKED = ?",
                new SerializerWriteBasicExt(datas, new int[] {0, 1})).exec(values);
        return updated == 1;
    }

    /** Deletes a shared ticket only if the requester still owns its lock. */
    public final boolean deleteSharedTicketIfOwned(final String id, final String owner) throws BasicException {
        Object[] values = new Object[] {id, owner};
        Datas[] datas = new Datas[] {Datas.STRING, Datas.STRING};
        int updated = new PreparedSentence(s,
                "DELETE FROM sharedtickets WHERE ID = ? AND LOCKED = ?",
                new SerializerWriteBasicExt(datas, new int[] {0, 1})).exec(values);
        return updated == 1;
    }

    /** Updates a restaurant shared ticket only while the requester owns the lock. */
    public final boolean updateRSharedTicketIfOwned(final String id, final TicketInfo ticket,
            int pickupid, final String owner) throws BasicException {
        Object[] values = new Object[] {id, ticket.getName(), ticket, pickupid, owner};
        Datas[] datas = new Datas[] {
            Datas.STRING, Datas.STRING, Datas.SERIALIZABLE, Datas.INT, Datas.STRING
        };
        int updated = new PreparedSentence(s,
                "UPDATE sharedtickets SET NAME = ?, CONTENT = ?, PICKUPID = ? "
                + "WHERE ID = ? AND LOCKED = ?",
                new SerializerWriteBasicExt(datas, new int[] {1, 2, 3, 0, 4})).exec(values);
        return updated == 1;
    }

    /** Updates a shared ticket only while the requester owns the lock. */
    public final boolean updateSharedTicketIfOwned(final String id, final TicketInfo ticket,
            int pickupid, final String owner) throws BasicException {
        Object[] values = new Object[] {
            id, ticket.getName(), ticket, ticket.getUser().getId(), pickupid, owner
        };
        Datas[] datas = new Datas[] {
            Datas.STRING, Datas.STRING, Datas.SERIALIZABLE,
            Datas.STRING, Datas.INT, Datas.STRING
        };
        int updated = new PreparedSentence(s,
                "UPDATE sharedtickets SET NAME = ?, CONTENT = ?, APPUSER = ?, PICKUPID = ? "
                + "WHERE ID = ? AND LOCKED = ?",
                new SerializerWriteBasicExt(datas, new int[] {1, 2, 3, 4, 0, 5})).exec(values);
        return updated == 1;
    }

    /**
     * For Restaurant View
     * @param id
     * @param ticket
     * @param pickupid
     * @throws BasicException
     */
    public final void insertRSharedTicket(
            final String id, 
            final TicketInfo ticket, 
            int pickupid) throws BasicException {
        
        Object[] values = new Object[] {
            id, 
            ticket.getName(), 
            ticket,
            ticket.getUser(),
            ticket.getPickupId(),
            ticket.getHost()                          
        };
        Datas[] datas;
        datas = new Datas[] {
            Datas.STRING, 
            Datas.STRING, 
            Datas.SERIALIZABLE, 
            Datas.STRING,            
            Datas.INT

        };
        new PreparedSentence(s
            , "INSERT INTO sharedtickets ("
                + "ID, "
                + "NAME, "
                + "CONTENT, "
                + "APPUSER, "                    
                + "PICKUPID) "
                + "VALUES (?, ?, ?, ?, ?)"                
            , new SerializerWriteBasicExt(datas, new int[] {0, 1, 2, 3, 4})).exec(values);                
    }    

    /**
     * 
     * @param id
     * @throws BasicException
     */
    public final void deleteSharedTicket(final String id) throws BasicException {

        new StaticSentence(s
            , "DELETE FROM sharedtickets WHERE ID = ?"
            , SerializerWriteString.INSTANCE).exec(id);      
    }

    /**
     *
     * @param Id
     * @return
     * @throws BasicException
     */
    public final Integer getPickupId(String Id) throws BasicException {
        
        if (Id == null) {
            return null; 
        } else {
            Object[]record = (Object[]) new StaticSentence(s
                    , "SELECT PICKUPID FROM sharedtickets WHERE ID = ?"
                    , SerializerWriteString.INSTANCE
                    , new SerializerReadBasic(new Datas[] {Datas.INT})).find(Id);
            return record == null ? 0 : (Integer)record[0];
        }
    } 


    public final String getUserId(final String id) throws BasicException {
        Object[] userID = (Object []) new StaticSentence(s
            , "SELECT APPUSER FROM sharedtickets WHERE ID = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {Datas.STRING})).find(id);
            if( userID == null ) {
                return null;
            } else {
                return (String) userID[0];
        }
    }
    
    public final String getLockState(final String id, String lockState) throws BasicException {
        Object[] state = (Object[]) new StaticSentence(s
            , "SELECT LOCKED FROM sharedtickets WHERE ID = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {
                Datas.STRING
            })).find(id);
        return state == null ? null : (String) state[0];
    }

    public final String getServer(final String id, String user) throws BasicException {
        Object[] server = (Object[]) new StaticSentence(s
            , "SELECT appuser FROM sharedtickets WHERE ID = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {
                Datas.STRING
            })).find(id);
        return server == null ? null : (String) server[0];
    }    
}
