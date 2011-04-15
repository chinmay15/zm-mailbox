/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap.legacy.entry;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.prov.ldap.entry.LdapSignatureBase;

/**
 * @author schemers
 */
public class LdapSignature extends LdapSignatureBase {

    private String mDn;

    public LdapSignature(Account acct, String dn, Attributes attrs, Provisioning prov) throws NamingException {
        super(acct,
              LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraSignatureName),
              LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraSignatureId),
              LegacyLdapUtil.getAttrs(attrs), prov);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
    

}
