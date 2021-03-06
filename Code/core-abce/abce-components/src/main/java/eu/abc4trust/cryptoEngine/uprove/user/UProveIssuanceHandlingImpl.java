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

package eu.abc4trust.cryptoEngine.uprove.user;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.datacontract.schemas._2004._07.abc4trust_uprove.ArrayOfUProveKeyAndTokenComposite;
import org.datacontract.schemas._2004._07.abc4trust_uprove.FirstIssuanceMessageComposite;
import org.datacontract.schemas._2004._07.abc4trust_uprove.IssuerParametersComposite;
import org.datacontract.schemas._2004._07.abc4trust_uprove.SecondIssuanceMessageComposite;
import org.datacontract.schemas._2004._07.abc4trust_uprove.ThirdIssuanceMessageComposite;
import org.datacontract.schemas._2004._07.abc4trust_uprove.UProveKeyAndTokenComposite;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfstring;

import eu.abc4trust.abce.external.user.UserAbcEngine;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManager;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManagerException;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.cryptoEngine.uprove.util.UProveKeyAndToken;
import eu.abc4trust.cryptoEngine.uprove.util.UProveUtils;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.returnTypes.IssuMsgOrCredDesc;
import eu.abc4trust.smartcard.CardStorage;
import eu.abc4trust.smartcard.HardwareSmartcard;
import eu.abc4trust.smartcard.SmartcardStatusCode;
import eu.abc4trust.util.MyAttribute;
import eu.abc4trust.util.MyCredentialDescription;
import eu.abc4trust.util.MyCredentialSpecification;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.CredentialDescription;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.CryptoParams;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.NonRevocationEvidence;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.util.XmlUtils;

public class UProveIssuanceHandlingImpl implements UProveIssuanceHandling {

    private final UProveUtils utils;
    private final CredentialManager credManager;
    private final KeyManager keyManager;
    private final CardStorage cardStorage;
    private final CryptoEngineContext ctxt;
    private final UserAbcEngine userEngine;
    private final ReloadTokensCommunicationStrategy reloadTokens;
	public static boolean STORE_TOKENS_ON_SMARTCARD = false;

    @Inject
    public UProveIssuanceHandlingImpl(CryptoEngineContext ctxt, KeyManager keyManager, CredentialManager credManager,
            CardStorage cardStorage, UserAbcEngine userEngine, ReloadTokensCommunicationStrategy reloadTokens) {
        this.utils = new UProveUtils();
        this.ctxt = ctxt;
        this.credManager = credManager;
        this.keyManager = keyManager;
        this.cardStorage = cardStorage;
        this.userEngine = userEngine;
        this.reloadTokens = reloadTokens;
    }

    /* (non-Javadoc)
     * @see eu.abc4trust.cryptoEngine.uprove.user.UProveIssuanceHandling#issuanceProtocolStep(eu.abc4trust.xml.IssuanceMessage, java.net.URI)
     */
    @Override
    public IssuMsgOrCredDesc issuanceProtocolStep(IssuanceMessage m, URI prevCredentialUri)
            throws CryptoEngineException {
        ObjectFactory of = new ObjectFactory();
        org.datacontract.schemas._2004._07.abc4trust_uprove.ObjectFactory uproveOf = new org.datacontract.schemas._2004._07.abc4trust_uprove.ObjectFactory();
        IssuMsgOrCredDesc imoc = new IssuMsgOrCredDesc();

        // Fetch needed objects from the received IssuanceMessage...
        URI context = m.getContext(); // get the context
        Object o = m.getAny().get(0);

        if(o instanceof Element) {
            Element element = (Element) o;
            String elementName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();

            // This is for the first step in the UProve issuance protocol for the user side, the issuer side has returned a FirstIssuanceMessage object
            if(elementName.equalsIgnoreCase("firstIssuanceMessageComposite")) {
                FirstIssuanceMessageComposite firstIssuanceMessageComposite = this.utils.convertW3DomElementToJAXB(FirstIssuanceMessageComposite.class, (Element)o);

                @SuppressWarnings("unchecked")
                IssuancePolicy ip = ((JAXBElement<IssuancePolicy>) m.getAny().get(1)).getValue();
                Integer numberOfTokensParam = this.utils.convertW3DOMElementToIntegerParam("NumberOfTokensParam" , (Element)m.getAny().get(2) );

                List<MyAttribute> attrList = this.extractAttributtes(m);

                IssuerParameters issuerParameters = null;
                try {
                    issuerParameters = this.keyManager.getIssuerParameters(ip.getCredentialTemplate().getIssuerParametersUID());
                } catch (Exception e) {
                    throw new RuntimeException("failed to get issuer params from keyManager", e);
                }


                // Convert IssuerParameters to IssuerParametersComposite for
                // UProve Webservice interop compatibility
                IssuerParametersComposite ipc = this.utils.convertIssuerParameters(issuerParameters);

                ArrayOfstring arrayOfStringAttributesParam = this.utils.convertAttributesToUProveAttributes(ipc, attrList);

                URI scURI = UProveUtils.getSmartcardUri(this.cardStorage);
                String sessionKey = null;

                //				SystemParameters syspars;
                //		        try {
                //		            syspars = this.keyManager.getSystemParameters();
                //		        } catch (KeyManagerException ex) {
                //		            throw new RuntimeException(ex);
                //		        }

                int keyLength = 2048; //new UProveSystemParameters(syspars).getKeyLength();
                if(scURI != null){
                    HardwareSmartcard sc = (HardwareSmartcard) this.cardStorage.getSmartcard(scURI);
                    byte credID;
                    if(prevCredentialUri != null){
                        credID = sc.getCredentialIDFromUri(this.cardStorage.getPin(scURI), prevCredentialUri);
                    }else{
                        credID = sc.getCredentialIDFromUri(this.cardStorage.getPin(scURI), this.ctxt.uidOfIssuedCredentialCache.get(context));
                    }
                    sessionKey = this.utils.getSessionKey(this.ctxt.binding, this.cardStorage, credID, keyLength);
                }else{
                    sessionKey = this.utils.getSessionKey(this.ctxt.binding, keyLength);
                }
                this.ctxt.sessionKeyCache.put(context, sessionKey);

                this.ctxt.binding.verifyIssuerParameters(ipc, sessionKey);

                // Prover side generates second message based on the first message

                SecondIssuanceMessageComposite secondMessage = this.ctxt.binding.getSecondMessage(arrayOfStringAttributesParam, ipc, numberOfTokensParam, firstIssuanceMessageComposite, sessionKey); // utils.convertFirstIssuanceMessage(firstMessage));

                // Map secondMessage to abc:IssuanceMessage new xml elements along with the attributes, numberOfTokens and the IssuancePolicy
                IssuanceMessage ret = of.createIssuanceMessage();
                JAXBElement<SecondIssuanceMessageComposite> createSecondIssuanceMessageComposite = uproveOf.createSecondIssuanceMessageComposite(secondMessage);
                Element convertJAXBToW3DOMElement = this.utils.convertJAXBToW3DOMElement(createSecondIssuanceMessageComposite);
                ret.getAny().add(convertJAXBToW3DOMElement);
                ret.setContext(context);

                try{System.out.println("Second message from user to issuer: " + XmlUtils.toXml(of.createIssuanceMessage(ret)));}catch(Exception e){e.printStackTrace();}

                imoc.im = ret;
            } else if(elementName.equalsIgnoreCase("thirdIssuanceMessageComposite")) {
                // This is for the third and final step in the UProve issuance protocol for the user side, the issuer side has returned a ThirdIssuanceMessage object.
                ThirdIssuanceMessageComposite thirdIssuanceMessageComposite = this.utils.convertW3DomElementToJAXB(ThirdIssuanceMessageComposite.class, element);

                MyCredentialDescription myCredDesc;
                try {
                    myCredDesc = new MyCredentialDescription(
                            (CredentialDescription) XmlUtils.unwrap(m.getAny().get(1),
                                    CredentialDescription.class), this.keyManager);
                } catch(KeyManagerException kme) {
                    throw new CryptoEngineException(kme);
                }

                // Prover side generates Tokens based on the third message
                // System.out.println(thirdMessage.getSessionKey());
                // for (byte[] a : thirdMessage.getSigmaR()) {
                // System.out.println(Arrays.toString(a));
                // }

                //Get a Session key using credID - then store the real credential on the card later.

                String sessionKey = null;
                if (!this.ctxt.sessionKeyCache.containsKey(context)) {
                    throw new RuntimeException("Session key not found for third issuance message.");
                } else {
                    sessionKey = this.ctxt.sessionKeyCache.get(context);
                }

                URI issuerParamsUid = myCredDesc.getCredentialDescription()
                        .getIssuerParametersUID();
                IssuerParameters issuerParameters;
                try {
                    issuerParameters = this.keyManager
                            .getIssuerParameters(issuerParamsUid);
                } catch (KeyManagerException ex) {
                    throw new CryptoEngineException(ex);
                }
                IssuerParametersComposite ipc = this.utils
                        .convertIssuerParameters(issuerParameters);
                this.ctxt.binding.verifyIssuerParameters(ipc, sessionKey);


                // ArrayList<UProveKeyAndToken> compositeTokens == one ABC4Trust Credential with array size amount of usage before the credential must be renewed (Minus Tokens saved for a scope))

                ArrayOfUProveKeyAndTokenComposite compositeTokens = this.ctxt.binding
                        .generateTokens(thirdIssuanceMessageComposite, sessionKey);

                this.ctxt.binding.printDebugOutputFromUProve();
                if (compositeTokens == null) {
                    throw new RuntimeException("failed to issue tokens (binding returned null)");
                }
                URI scURI = UProveUtils.getSmartcardUri(this.cardStorage);
                if(scURI != null){
                    HardwareSmartcard sc = (HardwareSmartcard) this.cardStorage.getSmartcard(scURI);
                    //Only issue when it is a new credential as we otherwise already have a status of 2 on the old credential.
                    byte credID;
                    if(prevCredentialUri == null){
                        credID = sc.getCredentialIDFromUri(this.cardStorage.getPin(scURI), this.ctxt.uidOfIssuedCredentialCache.get(context));
                        SmartcardStatusCode sStatus = sc.issueCredentialOnSmartcard(this.cardStorage.getPin(scURI), credID);
                        if (sStatus != SmartcardStatusCode.OK) {
                            throw new RuntimeException("failed to issue credential on smartcard");
                        }
                    }
                }
                CredentialSpecification credSpec = null;
                try {
                    credSpec = this.keyManager
                            .getCredentialSpecification(myCredDesc
                                    .getCredentialDescription()
                                    .getCredentialSpecificationUID());
                } catch (KeyManagerException ex) {
                    throw new CryptoEngineException(ex);
                }
                MyCredentialSpecification myCredSpec = new MyCredentialSpecification(
                        credSpec);

                NonRevocationEvidence nre = null;

                if (credSpec.isRevocable()) {
                    try{
                        nre = (NonRevocationEvidence) XmlUtils.unwrap(m.getAny()
                                .get(2), NonRevocationEvidence.class);
                    }catch(Exception e){
                        System.out.println("Assuming re-issuance");
                    }

                    List<UProveKeyAndTokenComposite> uProveKeyAndTokenComposites = compositeTokens
                            .getUProveKeyAndTokenComposite();

                    // TODO(jdn): Check revocation handle.
                    @SuppressWarnings("unused")
                    UProveKeyAndTokenComposite uProveKeyAndTokenComposite = uProveKeyAndTokenComposites
                    .get(0);
                    @SuppressWarnings("unused")
                    BigInteger revocationAttrValue = BigInteger.ZERO;
                    // userRevocation
                    // .compareRevocationHandleToNonRevocationEvidence(
                    // revocationAttrValue, nre);
                }
                Credential cred = of.createCredential();

                // Find the URI of the secret.
                URI secretUid = this.ctxt.secretCache.get(context);

                CredentialDescription cd = myCredDesc
                        .getCredentialDesc();
                cd.setCredentialUID(this.ctxt.uidOfIssuedCredentialCache
                        .get(context));
                cd.setSecretReference(secretUid);
                cred.setCredentialDescription(cd);

                // add user's secret attributes to the credential description.
                if (cd.getAttribute().size() != this.ctxt.attributeCache.get(context)
                        .size()) {
                    for (MyAttribute attr : this.ctxt.attributeCache.get(context)) {
                        if (!cd.getAttribute().contains(attr)) {
                            // first add the friendly description from the spec.
                            List<FriendlyDescription> frienlyAttrDescFromSpec = myCredSpec
                                    .getFriendlyDescryptionsForAttributeType(attr
                                            .getType());
                            if (attr.getFriendlyAttributeName() == null) {
                                attr.getFriendlyAttributeName()
                                .addAll(frienlyAttrDescFromSpec);
                            }
                            cd.getAttribute().add(attr.getXmlAttribute());
                        }
                    }
                }

                CryptoParams cryptoEvidence = of.createCryptoParams();
                //order of elements in cryptoEvidence.getAny() is used in other classes.
                //order is
                // 0. tokens
                // (OPTIONAL) 1. revocation evidence
                // 2. issuer parameters

                if(STORE_TOKENS_ON_SMARTCARD ){
                	//Convert ArrayOfUProveKeyAndTokenComposite to serializable version
                	cryptoEvidence.getAny().add(utils.convertArrayOfUProveKeyAndTokenComposite(compositeTokens));
                }else{
	                //Add empty list since logic dictates it has to be there.
	                //TODO: Make this nicer so as to avoid misunderstandings. Not nice to have an always empty array.
	                cryptoEvidence.getAny().add(new ArrayList<UProveKeyAndToken>());
                }

                // Set NonRevocationEvidenceUID.
                if (credSpec.isRevocable() && (nre != null)) {
                    nre.setCredentialUID(myCredDesc.getUid());
                    cred.getNonRevocationEvidenceUID().add(
                            nre.getNonRevocationEvidenceUID());
                    cryptoEvidence.getAny().add(nre);
                }

                cred.setCryptoParams(cryptoEvidence);

                @SuppressWarnings("unused")
                URI newCredentialUri = null;
                try {
                    if(prevCredentialUri == null){
                        List<URI> credUids = new ArrayList<URI>();
                        credUids.add(cred.getCredentialDescription().getCredentialUID());
                        List<URI> pseudonyms = new ArrayList<URI>();
                        ReloadInformation info = new ReloadInformation(credUids, pseudonyms);
                        this.reloadTokens.setCredentialInformation(context, info);
                        newCredentialUri = this.credManager.storeCredential(cred);

                        if(!STORE_TOKENS_ON_SMARTCARD){
	                        try {
	                            URI credUid = cred.getCredentialDescription().getCredentialUID();
	                            this.keyManager.storeCredentialTokens(credUid, this.utils.convertArrayOfUProveKeyAndTokenComposite(compositeTokens));
	                        } catch (KeyManagerException e) {
	                            throw new CryptoEngineException(e);
	                        }
                        }
                    }
                } catch (CredentialManagerException ex) {
                    throw new CryptoEngineException(ex);
                }
                imoc.cd = cred.getCredentialDescription();
                imoc.im = null;

                if (prevCredentialUri!=null) {
                    Credential oldCred = null;
                    try {
                        oldCred = this.credManager.getCredential(prevCredentialUri);
                    } catch (CredentialManagerException e1) {
                        throw new CryptoEngineException(e1);
                    }
                    imoc.cd = oldCred.getCredentialDescription();
                    if(!STORE_TOKENS_ON_SMARTCARD){
	                    try {
	                        URI credUid = oldCred.getCredentialDescription().getCredentialUID();
	                        this.keyManager.storeCredentialTokens(credUid, this.utils.convertArrayOfUProveKeyAndTokenComposite(compositeTokens));
	                    } catch (KeyManagerException e) {
	                        throw new CryptoEngineException(e);
	                    }
                    }else{                    
                    	//Update the old credential with the newly created Uprove tokens
                    	Object tokens_as_object = oldCred.getCryptoParams().getAny().get(0);
                    	@SuppressWarnings("unchecked")
                    	ArrayList<UProveKeyAndToken> old_tokens = (ArrayList<UProveKeyAndToken>) XmlUtils.unwrap(tokens_as_object, ArrayList.class);
                    	System.out.println("old tokens size before adding: " + old_tokens.size());
                    	old_tokens.addAll(utils.convertArrayOfUProveKeyAndTokenComposite(compositeTokens));
                    	System.out.println("old tokens size after adding: " + old_tokens.size());

                    	try {
                    		credManager.updateCredential(oldCred);
                    	} catch (CredentialManagerException e1) {
                    		throw new CryptoEngineException(e1);
                    	}
                    }
                }
            } else {
                throw new IllegalStateException("IssuanceMessage from Issuer could not be handled : unknown Element !" + o.getClass() + " : "+ element + ":" +element.getNodeName());
            }
        } else {
            throw new IllegalStateException("IssuanceMessage from Issuer could not be handled!" + o.getClass());
        }
        return imoc;
    }

    private List<MyAttribute> extractAttributtes(IssuanceMessage m) {
        int ix = 3;
        List<MyAttribute> attrList = new ArrayList<MyAttribute>();
        while(ix < m.getAny().size()) {
            @SuppressWarnings("unchecked")
            JAXBElement<Attribute> jaxb = (JAXBElement<Attribute>)m.getAny().get(ix);
            Attribute a = jaxb.getValue();
            attrList.add(new MyAttribute(a));
            ix++;
        }
        return attrList;
    }

}
