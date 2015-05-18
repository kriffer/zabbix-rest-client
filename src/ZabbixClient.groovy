
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.log4j.LogManager
import org.apache.log4j.Logger

/**
 * Contains main Zabbix methods
 */
class ZabbixClient {

	final static Logger log = LogManager.getLogger(ZabbixClient.class.getName())

	static String user;
	static String password;
	static String url;
	private String authData;

	ZabbixClient(user, password, url) {
		this.user = user
		this.password = password
		this.url = url
		auth()
	}

	/**
	 * Main method for making a query to Zabbix
	 * @param query
	 * @return
	 */
	public def query(JsonBuilder query) throws Exception {
		if (!authData && query.content.method != 'user.login') auth();
		query.content.auth = authData
		query.content.jsonrpc = '2.0'

		RESTClient client = new RESTClient(url)
		def resp = client.post(
				contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				path: "/zabbix/api_jsonrpc.php",
				body: query.toString()
		);
		return resp.data;
	}

	/**
	 * Gets a Zabbix auth data key
	 */
	protected void auth() throws Exception {
		JsonBuilder jb = new JsonBuilder()
		jb {
			method 'user.login'
			params {
				user user
				password password
			}
			id '1'
		}
		authData = query(jb).result;
	}

	/**
	 * Common method for the different operations with Zabbix objects
	 * @param json
	 * @return
	 * @throws Exception
	 */
	public def operateZabbixObjects(JsonBuilder json) throws Exception {
		query(json)
	}

	/**
	 * Acknowledges Zabbix event
	 * @param evId event id
	 * @param commit text of acknowledge message
	 * @return
	 */
	public def zabbixEventAcknowledge(evId, commit) throws Exception {
		def json = new JsonBuilder()
		json {
			jsonrpc "2.0"
			method "event.acknowledge"
			params {
				eventids "${evId}"
				message commit
			}
			auth authData
			id '1'
		}
		operateZabbixObjects(json)
	}

	/**
	 * Creates maintenance in Zabbix
	 * @param maintenanceName
	 * @param since
	 * @param till
	 * @param host
	 * @param descr
	 * @param per
	 * @return
	 */
	public def setMaintenance(maintenanceName, since, till, host, descr, per) throws Exception {
		log.info('New maintenance name: ' + maintenanceName)
		def json = new JsonBuilder()
		json {
			jsonrpc "2.0"
			method "maintenance.create"

			params {
				name maintenanceName
				active_since since
				active_till till
				hostids host
				description descr
				timeperiods([
						{
							period per
						}
				])
			}
			auth authData
			id 1
		}
		operateZabbixObjects(json).result.maintenanceids[0]
	}

	/**
	 * Deletes maintenance
	 * @param i - maintenance id
	 * @return
	 */
	public def deleteMaintenance(String i) throws Exception {
		log.info("Deleting maintenance with id: " + i)
		def json = new JsonBuilder()
		j {
			jsonrpc "2.0"
			method "maintenance.delete"
			params([
					i
			])
			auth authData
			id 1
		}
		operateZabbixObjects(json).result

	}

	/**
	 * Getting maintenanceid by name
	 * @param name - maintenance name
	 * @return
	 */
	public def getMaintenanceId(name) throws Exception {
		def json = new JsonBuilder()
		json {
			jsonrpc "2.0"
			method "maintenance.get"

			params {
				output "extend"

			}
			auth authData
			id 1
		}
		def maintenanceId = null
		def res = operateZabbixObjects(json).result
		for (it in res) {
			println(it.maintenanceid + " - " + it.name)
			if (it.name.contains(name)) {
				maintenanceId = it.maintenanceid
				break
			}
		}
		maintenanceId
	}

	/**
	 * Checks whether maintenance exists
	 * @return
	 */
	public boolean existsMaintenance(String n) throws Exception {
		def j = new JsonBuilder()
		j {
			jsonrpc "2.0"
			method "maintenance.exists"
			params {
				name n
			}
			auth authData
			id 1
		}
		boolean exist = query(j).result
		log.info("Existing flag: " + exist)
		return exist
	}

	/**
	 * Getting hostid by name
	 * @param currentHost - name of host
	 * @return
	 */
	public def getCurrentHost(currentHost) throws Exception {
		def json = new JsonBuilder()
		json {
			jsonrpc "2.0"
			method "host.get"
			params {
				output "extend",
						filter {
							host currentHost
						}
			}
			auth authData
			id 1
		}
		operateZabbixObjects(json).result.hostid
	}
}
