package softwarepoets.stldroid;

import java.io.StreamTokenizer;

import toxi.geom.mesh.Mesh3D;

public class STLAsciiReader {
	
	private StreamTokenizer tokenizer;

	private Mesh3D mesh;
	
	interface ParserState {
		ParserState next() throws IllegalStateException;
	}
	
	class HeadState implements ParserState {

		@Override
		public ParserState next() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	class FacetState implements ParserState {

		@Override
		public ParserState next() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	class FinalState implements ParserState {

		@Override
		public ParserState next() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
