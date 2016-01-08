/**
 * Copyright (C) 2011-2013 Incapture Technologies LLC
 * 
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */

/**
 * This file is autogenerated and any changes will be overwritten.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DotNetRaptureAPI;
using DotNetRaptureAPI.Common;
using DotNetRaptureAPI.Common.FixedTypes;
using DotNetRaptureAPI.Utils;

namespace DotNetRaptureAPI.notification
{

public class HttpNotificationApi : BaseHttpApi , NotificationApi, ScriptNotificationApi {
	public HttpNotificationApi(HttpLoginApi login) : base(login, "notification") {
	
	}
		
	   
	    public List<RaptureNotificationConfig> getNotificationManagerConfigs(CallingContext context)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            GetNotificationManagerConfigsPayload requestObj = new GetNotificationManagerConfigsPayload();
	            string responseObjectJson;
	            responseObjectJson = makeRequest("GETNOTIFICATIONMANAGERCONFIGS", RaptureSerializer.SerializeJson<GetNotificationManagerConfigsPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<List<RaptureNotificationConfig>>(resp.response.content);
	            }
	             throw new Exception("Error in getNotificationManagerConfigs, no response returned");
	        }
	   
	    public List<RaptureFolderInfo> listNotificationsByUriPrefix(CallingContext context,  string uriPrefix)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            ListNotificationsByUriPrefixPayload requestObj = new ListNotificationsByUriPrefixPayload();
	        requestObj.uriPrefix = uriPrefix;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("LISTNOTIFICATIONSBYURIPREFIX", RaptureSerializer.SerializeJson<ListNotificationsByUriPrefixPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<List<RaptureFolderInfo>>(resp.response.content);
	            }
	             throw new Exception("Error in listNotificationsByUriPrefix, no response returned");
	        }
	   
	    public List<RaptureNotificationConfig> findNotificationManagerConfigsByPurpose(CallingContext context,  string purpose)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            FindNotificationManagerConfigsByPurposePayload requestObj = new FindNotificationManagerConfigsByPurposePayload();
	        requestObj.purpose = purpose;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("FINDNOTIFICATIONMANAGERCONFIGSBYPURPOSE", RaptureSerializer.SerializeJson<FindNotificationManagerConfigsByPurposePayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<List<RaptureNotificationConfig>>(resp.response.content);
	            }
	             throw new Exception("Error in findNotificationManagerConfigsByPurpose, no response returned");
	        }
	   
	    public RaptureNotificationConfig createNotificationManager(CallingContext context,  string notificationManagerUri,  string config,  string purpose)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            CreateNotificationManagerPayload requestObj = new CreateNotificationManagerPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	        requestObj.config = config;
	        requestObj.purpose = purpose;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("CREATENOTIFICATIONMANAGER", RaptureSerializer.SerializeJson<CreateNotificationManagerPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<RaptureNotificationConfig>(resp.response.content);
	            }
	             throw new Exception("Error in createNotificationManager, no response returned");
	        }
	   
	    public bool notificationManagerExists(CallingContext context,  string notificationManagerUri)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            NotificationManagerExistsPayload requestObj = new NotificationManagerExistsPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("NOTIFICATIONMANAGEREXISTS", RaptureSerializer.SerializeJson<NotificationManagerExistsPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<bool>(resp.response.content);
	            }
	             throw new Exception("Error in notificationManagerExists, no response returned");
	        }
	   
	    public RaptureNotificationConfig getNotificationManagerConfig(CallingContext context,  string notificationManagerUri)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            GetNotificationManagerConfigPayload requestObj = new GetNotificationManagerConfigPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("GETNOTIFICATIONMANAGERCONFIG", RaptureSerializer.SerializeJson<GetNotificationManagerConfigPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<RaptureNotificationConfig>(resp.response.content);
	            }
	             throw new Exception("Error in getNotificationManagerConfig, no response returned");
	        }
	   
	    public void deleteNotificationManager(CallingContext context,  string notificationManagerUri)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            DeleteNotificationManagerPayload requestObj = new DeleteNotificationManagerPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("DELETENOTIFICATIONMANAGER", RaptureSerializer.SerializeJson<DeleteNotificationManagerPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<void>(resp.response.content);
	            }
	             throw new Exception("Error in deleteNotificationManager, no response returned");
	        }
	   
	    public long getLatestNotificationEpoch(CallingContext context,  string notificationManagerUri)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            GetLatestNotificationEpochPayload requestObj = new GetLatestNotificationEpochPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("GETLATESTNOTIFICATIONEPOCH", RaptureSerializer.SerializeJson<GetLatestNotificationEpochPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<long>(resp.response.content);
	            }
	             throw new Exception("Error in getLatestNotificationEpoch, no response returned");
	        }
	   
	    public string publishNotification(CallingContext context,  string notificationManagerUri,  string referenceId,  string content,  string contentType)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            PublishNotificationPayload requestObj = new PublishNotificationPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	        requestObj.referenceId = referenceId;
	        requestObj.content = content;
	        requestObj.contentType = contentType;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("PUBLISHNOTIFICATION", RaptureSerializer.SerializeJson<PublishNotificationPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<string>(resp.response.content);
	            }
	             throw new Exception("Error in publishNotification, no response returned");
	        }
	   
	    public NotificationResult findNotificationsAfterEpoch(CallingContext context,  string notificationManagerUri,  long epoch)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            FindNotificationsAfterEpochPayload requestObj = new FindNotificationsAfterEpochPayload();
	        requestObj.notificationManagerUri = notificationManagerUri;
	        requestObj.epoch = epoch;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("FINDNOTIFICATIONSAFTEREPOCH", RaptureSerializer.SerializeJson<FindNotificationsAfterEpochPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<NotificationResult>(resp.response.content);
	            }
	             throw new Exception("Error in findNotificationsAfterEpoch, no response returned");
	        }
	   
	    public NotificationInfo getNotification(CallingContext context,  string notificationUri,  string id)
	        {
	            System.Web.Script.Serialization.JavaScriptSerializer oSerializer = 
	         new System.Web.Script.Serialization.JavaScriptSerializer();
	            oSerializer.RegisterConverters(new System.Web.Script.Serialization.JavaScriptConverter[] { new JsonContentConverter() });
	            GetNotificationPayload requestObj = new GetNotificationPayload();
	        requestObj.notificationUri = notificationUri;
	        requestObj.id = id;
	            string responseObjectJson;
	            responseObjectJson = makeRequest("GETNOTIFICATION", RaptureSerializer.SerializeJson<GetNotificationPayload>(requestObj));
	            if (responseObjectJson != null && responseObjectJson.Length > 0) {
	              GeneralResponse resp = GeneralResponseConverter.retrieveFromString(responseObjectJson);
	              return RaptureSerializer.DeserializeJson<NotificationInfo>(resp.response.content);
	            }
	             throw new Exception("Error in getNotification, no response returned");
	        }
	
	       public List<RaptureNotificationConfig> getNotificationManagerConfigs() {
	            return getNotificationManagerConfigs(null);
	       }

	       public List<RaptureFolderInfo> listNotificationsByUriPrefix(string uriPrefix) {
	            return listNotificationsByUriPrefix(null,uriPrefix);
	       }

	       public List<RaptureNotificationConfig> findNotificationManagerConfigsByPurpose(string purpose) {
	            return findNotificationManagerConfigsByPurpose(null,purpose);
	       }

	       public RaptureNotificationConfig createNotificationManager(string notificationManagerUri, string config, string purpose) {
	            return createNotificationManager(null,notificationManagerUri,config,purpose);
	       }

	       public bool notificationManagerExists(string notificationManagerUri) {
	            return notificationManagerExists(null,notificationManagerUri);
	       }

	       public RaptureNotificationConfig getNotificationManagerConfig(string notificationManagerUri) {
	            return getNotificationManagerConfig(null,notificationManagerUri);
	       }

	       public void deleteNotificationManager(string notificationManagerUri) {
	            return deleteNotificationManager(null,notificationManagerUri);
	       }

	       public long getLatestNotificationEpoch(string notificationManagerUri) {
	            return getLatestNotificationEpoch(null,notificationManagerUri);
	       }

	       public string publishNotification(string notificationManagerUri, string referenceId, string content, string contentType) {
	            return publishNotification(null,notificationManagerUri,referenceId,content,contentType);
	       }

	       public NotificationResult findNotificationsAfterEpoch(string notificationManagerUri, long epoch) {
	            return findNotificationsAfterEpoch(null,notificationManagerUri,epoch);
	       }

	       public NotificationInfo getNotification(string notificationUri, string id) {
	            return getNotification(null,notificationUri,id);
	       }

}
}


