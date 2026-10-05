--    KylGis POS Punto de Venta Táctil
--    Copyright (c) 2026 KylGis POS
--    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
--    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
--    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
--
--    This file is part of KylGis POS
--
--    KylGis POS is free software: you can redistribute it and/or modify
--    it under the terms of the GNU General Public License as published by
--    the Free Software Foundation, either version 3 of the License, or
--    (at your option) any later version.
--
--    KylGis POS is distributed in the hope that it will be useful,
--    but WITHOUT ANY WARRANTY; without even the implied warranty of
--    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--    GNU General Public License for more details.
--
--    You should have received a copy of the GNU General Public License
--    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
/*
 * Script created by Jack, uniCenta 20/11/2016 08:00:00
 *
 * Create ORDERS table for Remote Display
*/

/* Header line. Object: orders. Script date: 01/01/2017 00:00:01. */
CREATE TABLE IF NOT EXISTS `orders` (
    `id` int(11) NOT NULL,
    `orderid` varchar(50) DEFAULT NULL,
    `qty` int(11) DEFAULT '1',
    `details` varchar(255) DEFAULT NULL,
    `attributes` varchar(255) DEFAULT NULL,
    `notes` varchar(255) DEFAULT NULL,
    `ticketid` varchar(50) DEFAULT NULL,
    `ordertime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `displayid` int(11) DEFAULT '1',
    `auxiliary` int(11) DEFAULT NULL,
    `completetime` timestamp,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT = Compact;
 