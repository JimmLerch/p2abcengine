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

package eu.abc4trust.returnTypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.PseudonymMetadata;

@XmlRootElement(name="UiIssuanceReturn", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class UiIssuanceReturn {
    /**
     * The context of the corresponding UiIssuanceArguments.
     */
    public URI uiContext;
    /**
     * The index of the chosen issuance token.
     */
    public int chosenIssuanceToken;
    /**
     * User-defined metadata for each newly created pseudonym, and for each metadata the user changed
     * through the GUI (the User can add free notes or descriptions to the pseudonym to remind her
     * later when re-using the pseudonym; this metadata will be stored with the corresponding
     * pseudonyms). The metadata given here will replace the old metadata.
     */
    public Map<String, PseudonymMetadata> metadataToChange;
    /**
     * Chosen pseudonym list.
     * If there are no pseudonyms to choose, put NULL here.
     */
    public Integer chosenPseudonymList;
    /**
     * Chosen inspectors: where inspectorChoice[chosenIssuanceToken][j].contains(chosenInspectors[j])
     */
    public List<String> chosenInspectors;
    /**
     * List of AttributeValues for the each of the self-claimed attributes.
     */
    //public List<Object> chosenAttributeValues;

    public UiIssuanceReturn(URI context, int chosenIssuanceToken, Map<String, PseudonymMetadata> metadataToChange,
            int chosenPseudonymList, List<String> chosenInspectors/*, List<Object> chosenAttributeValues*/) {
        this.uiContext = context;
        this.chosenIssuanceToken = chosenIssuanceToken;
        this.metadataToChange = metadataToChange;
        this.chosenPseudonymList = chosenPseudonymList;
        // shouldn't/cannot be null - create empty
        if(chosenInspectors==null) {
            this.chosenInspectors = new ArrayList<String>();
        } else {
            this.chosenInspectors = chosenInspectors;
        }
        //this.chosenAttributeValues = chosenAttributeValues;
    }

    public UiIssuanceReturn() {
        this.uiContext = null;
        this.chosenIssuanceToken = 0;
        this.metadataToChange = new HashMap<String, PseudonymMetadata>();
        this.chosenPseudonymList = 0;
        // when serializing as XML - this chosenInspectors could be null
        this.chosenInspectors = new ArrayList<String>();
        //this.chosenAttributeValues = null;
    }

    @Override
    public String toString() {
      return "UiIssuanceReturn [uiContext=" + uiContext + ", chosenIssuanceToken="
          + chosenIssuanceToken + ", metadataToChange=" + metadataToChange
          + ", chosenPseudonymList=" + chosenPseudonymList + ", chosenInspectors="
          + chosenInspectors + "]";
    }
}
