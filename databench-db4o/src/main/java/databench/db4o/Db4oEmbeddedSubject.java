package databench.db4o;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.config.EmbeddedConfiguration;

import databench.SingleVMBank;
import databench.database.FolderDatabase;

public class Db4oEmbeddedSubject extends Db4oSubject implements SingleVMBank {

	private static final long serialVersionUID = 1L;

	@Override
	public void tearDown() {
		db().close();
	}

	private final ObjectContainer db = Db4oEmbedded.openFile(configuration(),
			FolderDatabase.path() + "/db4o" + System.currentTimeMillis());

	private EmbeddedConfiguration configuration() {
		EmbeddedConfiguration conf = Db4oEmbedded.newConfiguration();
		conf.common().updateDepth(2);
		return conf;
	}

	@Override
	protected ObjectContainer db() {
		return db;
	}

}