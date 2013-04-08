// DISABLED
// This implementation can't scale for 500000 tasks / 50000 accounts.
// It never ends.

//package databench.db4o;
//
//import com.db4o.ObjectContainer;
//import com.db4o.ObjectServer;
//import com.db4o.cs.Db4oClientServer;
//import com.db4o.cs.config.ClientConfiguration;
//
//import databench.database.FolderDatabase;
//
//public class Db4oClientServerSubject extends Db4oSubject {
//
//	private static final long serialVersionUID = 1L;
//
//	private ObjectServer server = null;
//	private int port = 21344;
//	private String user = "user";
//	private String password = "password";
//
//	@Override
//	public Long[] setUp(Integer size) {
//		server = Db4oClientServer.openServer(
//				FolderDatabase.path() + "/db4o.bd", port);
//		server.grantAccess(user, password);
//		initializeDB();
//		return super.setUp(size);
//	}
//
//	private ObjectContainer db = null;
//
//	@Override
//	public void warmUp() {
//		initializeDB();
//		super.warmUp();
//	}
//
//	private void initializeDB() {
//		db = Db4oClientServer.openClient(configuration(), "localhost", port,
//				user, password);
//	}
//
//	private ClientConfiguration configuration() {
//		ClientConfiguration conf = Db4oClientServer.newClientConfiguration();
//		conf.common().updateDepth(2);
//		return conf;
//	}
//
//	@Override
//	protected ObjectContainer db() {
//		return db;
//	}
//
//	@Override
//	public void tearDown() {
//		db.close();
//	}
//
// }