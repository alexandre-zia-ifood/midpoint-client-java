/**
 * Copyright (c) 2017-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.client.api;

import com.evolveum.midpoint.client.api.exception.AuthenticationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 *
 */
public interface Service {
	
	// TODO
//	ProxyService proxy();

	ObjectCollectionService<UserType> users();

	<T> RpcService<T> rpc();

	ObjectCollectionService<ValuePolicyType> valuePolicies();
	
	UserType self() throws AuthenticationException;
	Service impersonate(String oid);
	Service addHeader(String header, String value);

	ObjectCollectionService<SecurityPolicyType> securityPolicies();

    ObjectCollectionService<ConnectorType> connectors();

	ObjectCollectionService<ConnectorHostType> connectorHosts();

	ObjectCollectionService<GenericObjectType> genericObjects();

	ObjectCollectionService<ResourceType> resources();

	ObjectCollectionService<ObjectTemplateType> objectTemplates();

	ObjectCollectionService<SystemConfigurationType> systemConfigurations();

	ObjectCollectionService<TaskType> tasks();

	ObjectCollectionService<ShadowType> shadows();

	ObjectCollectionService<RoleType> roles();

	ObjectCollectionService<OrgType> orgs();

	ServiceUtil util();
}
