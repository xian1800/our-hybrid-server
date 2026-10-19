/**
 *  HybridServer
 *  Copyright (C) 2025 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Launcher {
  public static void main(String[] args) {
    if (args.length > 1) {
      printUsage();
      return;
    }

    final Properties properties = new Properties();

    if (args.length == 1) {
      try (FileInputStream input = new FileInputStream(args[0])) {
        properties.load(input);
      } catch (IOException e) {
        System.err.println("Could not load configuration file '" + args[0] + "': " + e.getMessage());
        return;
      }
    }

    final HybridServer server;

    try {
      server = new HybridServer(properties);
    } catch (IllegalArgumentException e) {
      System.err.println("Invalid server configuration: " + e.getMessage());
      return;
    }

    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    server.start();
  }

  private static void printUsage() {
    System.err.println("Usage: java es.uvigo.esei.dai.hybridserver.Launcher [config.props]");
  }
}
