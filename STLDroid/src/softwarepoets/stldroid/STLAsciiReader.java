package softwarepoets.stldroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;

import toxi.geom.Vec3D;
import toxi.geom.mesh.Mesh3D;
import toxi.geom.mesh.TriangleMesh;

public class STLAsciiReader {

	class FacetState implements ParserState {
		@Override
		public ParserState next() throws IllegalStateException {
			try {
				t.nextToken();
				if (!"facet".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				t.nextToken();
				if (!"normal".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				t.nextToken();
				t.nextToken();
				t.nextToken();
				// vertex
				t.nextToken();
				if (!"outer".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				t.nextToken();
				if (!"loop".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				float[] vertex = new float[3];
				Vec3D[] vector = new Vec3D[3];
				for (int j = 0; j < 3; j++) {
					t.nextToken();
					if (!"vertex".equalsIgnoreCase(t.sval))
						throw new IllegalStateException(t.sval);
					t.nextToken();
					vertex[0] = Float.parseFloat(t.sval);
					t.nextToken();
					vertex[1] = Float.parseFloat(t.sval);
					t.nextToken();
					vertex[2] = Float.parseFloat(t.sval);
					vector[j] = new Vec3D(vertex);
				}
				tmpMesh.addFace(vector[0], vector[2], vector[1]);
				t.nextToken();
				if (!"endloop".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				t.nextToken();
				if (!"endfacet".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				
				// next state transition
				t.nextToken();
				if ("facet".equalsIgnoreCase(t.sval)) {
					t.pushBack();
					return new FacetState();
				}
				if ("endsolid".equalsIgnoreCase(t.sval)) {
					t.pushBack();
					return new FinalState();
				}
				throw new IllegalStateException();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage());
			} catch (NumberFormatException e) {
				throw new IllegalStateException(e.getMessage());
			}
		}
	}

	class FinalState implements ParserState {

		@Override
		public ParserState next() throws IllegalStateException {
			return null;
		}

	}

	public class HeadState implements ParserState {

		@Override
		public ParserState next() throws IllegalStateException {
			try {
				// solid
				t.nextToken();
				if (!"solid".equalsIgnoreCase(t.sval))
					throw new IllegalStateException(t.sval);
				// name
				t.nextToken();
				return new FacetState();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage());
			}
		}
	}

	public interface ParserState {
		ParserState next() throws IllegalStateException;
	}

	private StreamTokenizer t;

	

	private Mesh3D tmpMesh;

	private ParserState state = new HeadState();

	public Mesh3D load(InputStream fis) throws IllegalStateException {
		tmpMesh = new TriangleMesh();
		t = new StreamTokenizer(fis);
		t.resetSyntax();
		t.wordChars(33, 126);
		t.whitespaceChars(0, 32);
		
		while ( state != null) {
			state = state.next();
		}

		return tmpMesh;

	}

	public Mesh3D load(String fileName) throws FileNotFoundException, IllegalStateException {
		return load(new FileInputStream(fileName));
	}
}
