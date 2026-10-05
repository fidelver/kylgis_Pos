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
-- Database upgrade script for MySQL
-- v4.5 - v4.5.5 3JUNE2018

--
-- CLEAR THE DECKS
--
DELETE FROM sharedtickets;

set foreign_key_checks = 0;

-- RECREATE applications --
DROP TABLE `applications`;
CREATE TABLE IF NOT EXISTS `applications` (
	`id` varchar(255) NOT NULL,
	`name` varchar(255) NOT NULL,
	`version` varchar(255) NOT NULL,
	`instdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY  ( `id` )
) ENGINE = InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT = Compact;

-- SYSTEM
DELETE FROM resources WHERE id = '00';
INSERT INTO resources(id, name, restype, content) VALUES('00', 'Menu.Root', 0, $FILE{/com/mx/kylgis/pos/templates/Menu.Root.txt});

-- COMMONS
DELETE FROM roles WHERE id = '0';
INSERT INTO roles(id, name, permissions) VALUES('0', 'Administrator role', $FILE{/com/mx/kylgis/pos/templates/Role.Administrator.xml} );
DELETE FROM roles WHERE id = '1';
INSERT INTO roles(id, name, permissions) VALUES('1', 'Manager role', $FILE{/com/mx/kylgis/pos/templates/Role.Manager.xml} );
DELETE FROM roles WHERE id = '2';
INSERT INTO roles(id, name, permissions) VALUES('2', 'Employee role', $FILE{/com/mx/kylgis/pos/templates/Role.Employee.xml} );

set foreign_key_checks = 1;

INSERT INTO applications(id, name, version) VALUES($APP_ID{}, $APP_NAME{}, $APP_VERSION{});