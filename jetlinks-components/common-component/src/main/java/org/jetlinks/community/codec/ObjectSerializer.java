package org.jetlinks.community.codec;

import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

public interface ObjectSerializer {

    ObjectInput createInput(InputStream stream);

    ObjectOutput createOutput(OutputStream stream);


}
