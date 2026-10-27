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
package com.mx.kylgis.pos.customers;

import com.mx.kylgis.pos.util.StringUtils;
import java.awt.image.BufferedImage;
import java.io.Serializable;

/** @author jack gerrard, adrianromero */

// JG 20 Sep 12 Extended for Postal
// JG 2 Sep 13 Extended for Phone + Email
public class CustomerInfo implements Serializable {
    
    private static final long serialVersionUID = 9083257536541L;

    protected String id;
    protected String searchkey;
    protected String taxid;
    protected String name;
    protected String postal;
    protected String address;
    protected String address2;
    protected String city;
    protected String region;
    protected String country;
    protected String phone;
    protected String email;
    protected BufferedImage image; 
    protected Double curdebt;     

    
    /** Creates a new instance of CustomerInfo
     * @param id */
    public CustomerInfo(String id) {
        this.id = id;
        this.searchkey = null;
        this.taxid = null;
        this.name = null;
        this.postal = null;
        this.address = null;
        this.address2 = null;
        this.city = null;
        this.region = null;
        this.country = null;
        this.phone = null;
        this.email = null;
        this.image = null;
        this.curdebt = null;
    }
    
    /**
     *
     * @return id string
     */
    public String getId() {
        return id;
    }
    
    /**
     *
     * @return searchkey string
     */
    public String getSearchkey() {
        return searchkey;
    }
    public void setSearchkey(String searchkey) {
        this.searchkey = searchkey;
    }
    
    /**
     *
     * @return taxid string
     */
    public String getTaxid() {
        return taxid;
    }    
    public void setTaxid(String taxid) {
        this.taxid = taxid;
    }
    
    /**
     *
     * @return name string
     */
    public String getName() {
        return name;
    }   
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return postal/zip code string
     */
    public String getPcode() {
        return postal;
    }   
    public void setPcode(String postal) {
        this.postal = postal;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
    
    /**
     *
     * @return Primary Telephone string
     */
    public String getPhone1() {
        return phone;
    }
    public void setPhone1(String phone) {
        this.phone = phone;
    }

    /**
     *
     * @return email string
     */
    public String getCemail() {
        return email;
    }
    public void setCemail(String email) {
        this.email = email;
    }

    /**
     *
     * @return
     */
    public String printName() {
        return StringUtils.encodeXML(name);
    }
    
    @Override
    public String toString() {
        return getName();        
    }
   
    /**
     *
     * @return customer's photograph / image
    */
    public BufferedImage getImage() {
        return image;
    }
    /**
     *
     * @param img
     */
    public void setImage(BufferedImage img) {
        image = img;
    }

    /**
     *
     * @return name double
     */
    public Double getCurDebt() {
        return curdebt;
    }   
    public void setCurDebt(Double curdebt) {
        this.curdebt = curdebt;
    }    
}