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
package com.mx.kylgis.pos.scripting;

/**
 *
 * @author adrianromero
 * Created on 5 de marzo de 2007, 19:56
 *
 */
public class ScriptFactory {
    
    /**
     *
     */
    public static final String VELOCITY = "velocity";

    /**
     *
     */
    public static final String BEANSHELL = "beanshell";

    /**
     *
     */
    public static final String RHINO = "rhino";
    
    /** Creates a new instance of ScriptFactory */
    private ScriptFactory() {
    }
    
    /**
     *
     * @param name
     * @return
     * @throws ScriptException
     */
    public static ScriptEngine getScriptEngine(String name) throws ScriptException {
// JG 16 May use switch
        switch (name) {
            case VELOCITY:
                return new ScriptEngineVelocity();
            case BEANSHELL:
                return new ScriptEngineBeanshell();
    //        } else if (RHINO.equals(name)) {
    //            return new ScriptEngineRhino();
    //        } else if (name.startsWith("generic:")) {
    //            return new ScriptEngineGeneric(name.substring(8));
            default:
                throw new ScriptException("Script engine not found: " + name);
        }
    }    
}
