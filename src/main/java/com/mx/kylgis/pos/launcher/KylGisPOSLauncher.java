//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    KylGis POS implementation by Fidel Arcos.
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
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS. If not, see <http://www.gnu.org/licenses/>.

package com.mx.kylgis.pos.launcher;

import com.mx.kylgis.pos.forms.ApplicationIdentity;
import com.mx.kylgis.pos.forms.StartPOS;
import java.io.File;

public final class KylGisPOSLauncher {
    private KylGisPOSLauncher() {
    }

    public static void main(String[] args) {
        ApplicationIdentity.setCompatibilityFallback(
                "kylgispos", "KylGis POS", "KylGis POS");
        StartPOS.main(withDefaultConfig(args, "kylgispos.properties"));
    }

    static String[] withDefaultConfig(String[] args, String fileName) {
        if (args != null && args.length > 0) {
            return args;
        }
        File config = new File(System.getProperty("user.home"), fileName);
        return new String[] {config.getAbsolutePath()};
    }
}
