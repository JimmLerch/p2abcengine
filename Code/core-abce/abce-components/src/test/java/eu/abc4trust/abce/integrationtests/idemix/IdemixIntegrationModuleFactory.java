//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package eu.abc4trust.abce.integrationtests.idemix;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.google.inject.Module;

import eu.abc4trust.cryptoEngine.uprove.util.UProveUtils;
import eu.abc4trust.guice.ProductionModuleFactory;
import eu.abc4trust.guice.configuration.AbceConfiguration;
import eu.abc4trust.guice.configuration.AbceConfigurationImpl;
import eu.abc4trust.util.TemporaryFileFactory;

public class IdemixIntegrationModuleFactory {

  public static Module newModule(Random random, File storageFolder, String storagePrefix) {
    AbceConfiguration config = generateConfiguration(random, storageFolder, storagePrefix);
    return ProductionModuleFactory.newModule(config, ProductionModuleFactory.CryptoEngine.IDEMIX);
  }

  public static Module newModule(Random random) {
    return newModule(random, null, null);
  }

  private static AbceConfiguration generateConfiguration(Random random, File storageFolder,
      String filePrefix) {
    AbceConfigurationImpl config = new AbceConfigurationImpl();

    config.setCredentialFile(getStorageFile(storageFolder, filePrefix, "credential"));
    config.setKeyStorageFile(getStorageFile(storageFolder, filePrefix, "keys"));
    config.setSecretStorageFile(getStorageFile(storageFolder, filePrefix, "secrets"));
    config.setPseudonymsFile(getStorageFile(storageFolder, filePrefix, "pseudonyms"));
    config.setTokensFile(getStorageFile(storageFolder, filePrefix, "tokens"));
    config.setIssuerSecretKeyFile(getStorageFile(storageFolder, filePrefix, "issuersecretfile"));
    config.setIssuerLogFile(getStorageFile(storageFolder, filePrefix, "issuerlogfile"));
    config.setInspectorSecretKeyFile(getStorageFile(storageFolder, filePrefix, "inspectorsecretfile"));
    config.setRevocationAuthoritySecretStorageFile(getStorageFile(storageFolder, filePrefix, "revocationauthoritysecretfile"));
    config.setRevocationAuthorityStorageFile(getStorageFile(storageFolder, filePrefix, "revocationauthoritystoragefile"));
    config.setDefaultImagePath("file://error");
    config.setImageCacheDir(TemporaryFileFactory.createTemporaryDir());
    config.setPrng(random);
    
    //UProve configurations. They are not used, but is needed for the reloading setup within tests.
    config.setUProveRetryTimeout(3);
    config.setUProveNumberOfCredentialsToGenerate(2);
    config.setUProvePortNumber(new UProveUtils().getUserServicePort());
    config.setUProvePathToExe(new UProveUtils().getPathToUProveExe().getAbsolutePath());
    return config;
  }

  private static File getStorageFile(File storageFolder, String filePrefix, String storageName) {
    try {
      if (storageFolder == null) {
        return TemporaryFileFactory.createTemporaryFile();
      } else {

        File storageFile = new File(storageFolder, filePrefix + storageName);
        if (storageFile.exists()) {
          return storageFile;
        } else {
          boolean created = storageFile.createNewFile();
          if (!created) {
            throw new IOException("Could not create new file : " + storageFile.getName());
          }
          return storageFile;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
