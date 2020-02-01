package com.waridley.textroid.mongo.credentials.codecs;

import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CredentialCodec implements Codec<Credential> {
	
	private CodecRegistry codecRegistry;
	
	public CredentialCodec(CodecRegistry codecRegistry) {
		this.codecRegistry = codecRegistry;
	}
	
	@Override
	public Credential decode(BsonReader reader, DecoderContext decoderContext) {
		Credential credential;
		String userId = null;
		String identityProvider = null;
		Map<String, Object> additionalValues = new HashMap<>();
		
		String accessToken = null;
		String refreshToken = null;
		String userName = null;
		Integer expiresIn = null;
		List<String> scopes = new ArrayList<>();
		
		reader.readStartDocument();
		while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
			
			String fieldName = reader.readName();
			switch(fieldName) {
				case("userId"):
					userId = reader.readString();
					break;
				case("identityProvider"):
					identityProvider = reader.readString();
					break;
				case("accessToken"):
					accessToken = reader.readString();
					break;
				case("refreshToken"):
					refreshToken = reader.readString();
					break;
				case("userName"):
					userName = reader.readString();
					break;
				case("expiresIn"):
					expiresIn = reader.readInt32();
					break;
				case("scopes"):
					reader.readStartArray();
					while(reader.readBsonType() == BsonType.STRING) scopes.add(reader.readString());
					reader.readEndArray();
					break;
				default:
//					reader.skipValue();
					readAdditionalValues(reader, fieldName, decoderContext, additionalValues);
			}
			
		}
		reader.readEndDocument();
		
		if(accessToken != null) {
			credential = new OAuth2Credential(
					identityProvider,
					accessToken,
					refreshToken,
					userId,
					userName,
					expiresIn,
					scopes);
		} else if(additionalValues.size() > 0) {
			credential = new UnknownCredential(identityProvider, userId, additionalValues);
		} else {
			credential = new Credential(userId, identityProvider) { };
		}
		
		return credential;
	}
	
	@Override
	public void encode(BsonWriter writer, Credential credential, EncoderContext encoderContext) {
		if(credential instanceof OAuth2Credential) {
			OAuth2Credential cred = (OAuth2Credential) credential;
			writer.writeStartDocument();
			if(cred.getAccessToken() != null) {
				writer.writeName("accessToken");
				writer.writeString(cred.getAccessToken());
			}
			if(cred.getIdentityProvider() != null) {
				writer.writeName("identityProvider");
				writer.writeString(cred.getIdentityProvider());
			}
			if(cred.getRefreshToken() != null) {
				writer.writeName("refreshToken");
				writer.writeString(cred.getRefreshToken());
			}
			if(cred.getScopes() != null) {
				writer.writeName("scopes");
				writer.writeStartArray();
				List<String> scopes = cred.getScopes();
				for(String scope : scopes) {
					writer.writeString(scope);
				}
				writer.writeEndArray();
			}
			if(cred.getUserId() != null) {
				writer.writeName("userId");
				writer.writeString(cred.getUserId());
			}
			if(cred.getUserName() != null) {
				writer.writeName("userName");
				writer.writeString(cred.getUserName());
			}
			writer.writeEndDocument();
		} else {
			encodeCredentialInterface(writer, credential, encoderContext);
		}
		
	}
	
	private void encodeCredentialInterface(BsonWriter writer, Credential credential, EncoderContext encoderContext) {
		writer.writeStartDocument();
		if(credential.getUserId() != null) {
			writer.writeName("userId");
			writer.writeString(credential.getUserId());
		}
		if(credential.getIdentityProvider() != null) {
			writer.writeName("identityProvider");
			writer.writeString(credential.getIdentityProvider());
		}
		writer.writeEndDocument();
	}
	
	@Override
	public Class<Credential> getEncoderClass() {
		return Credential.class;
	}
	
	private void readAdditionalValues(BsonReader reader, String fieldName, DecoderContext decoderContext, Map<String, Object> additionalValues) {
		if(reader.getCurrentBsonType() == BsonType.OBJECT_ID) additionalValues.put(fieldName, reader.readObjectId());
		else if(reader.getCurrentBsonType() == BsonType.STRING) additionalValues.put(fieldName, reader.readString());
		else if(reader.getCurrentBsonType() == BsonType.INT32) additionalValues.put(fieldName, reader.readInt32());
		else if(reader.getCurrentBsonType() == BsonType.INT64) additionalValues.put(fieldName, reader.readInt64());
		else if(reader.getCurrentBsonType() == BsonType.DECIMAL128) additionalValues.put(fieldName, reader.readDecimal128());
		else if(reader.getCurrentBsonType() == BsonType.DOUBLE) additionalValues.put(fieldName, reader.readDouble());
		else if(reader.getCurrentBsonType() == BsonType.DOCUMENT) additionalValues.put(fieldName, decoderContext.decodeWithChildContext(codecRegistry.get(Document.class), reader));
		else if(reader.getCurrentBsonType() == BsonType.BOOLEAN) additionalValues.put(fieldName, reader.readBoolean());
		else if(reader.getCurrentBsonType() == BsonType.DATE_TIME) additionalValues.put(fieldName, reader.readDateTime());
		else if(reader.getCurrentBsonType() == BsonType.BINARY) additionalValues.put(fieldName, reader.readBinaryData());
		else if(reader.getCurrentBsonType() == BsonType.DB_POINTER) additionalValues.put(fieldName, reader.readDBPointer());
		else if(reader.getCurrentBsonType() == BsonType.JAVASCRIPT) additionalValues.put(fieldName, reader.readJavaScript());
		else if(reader.getCurrentBsonType() == BsonType.JAVASCRIPT_WITH_SCOPE) additionalValues.put(fieldName, reader.readJavaScriptWithScope());
		else if(reader.getCurrentBsonType() == BsonType.MAX_KEY) reader.readMaxKey();
		else if(reader.getCurrentBsonType() == BsonType.MIN_KEY) reader.readMinKey();
		else if(reader.getCurrentBsonType() == BsonType.REGULAR_EXPRESSION) additionalValues.put(fieldName, reader.readRegularExpression());
		else if(reader.getCurrentBsonType() == BsonType.TIMESTAMP) additionalValues.put(fieldName, reader.readTimestamp());
		else if(reader.getCurrentBsonType() == BsonType.SYMBOL) additionalValues.put(fieldName, reader.readSymbol());
		else if(reader.getCurrentBsonType() == BsonType.UNDEFINED) reader.readUndefined();
		else if(reader.getCurrentBsonType() == BsonType.NULL) {
			reader.readNull();
			additionalValues.put(fieldName, null);
		}
		else if(reader.getCurrentBsonType() == BsonType.ARRAY) {
			reader.readStartArray();
			BsonType type = reader.getCurrentBsonType();
			if(type == BsonType.STRING) while(reader.getCurrentBsonType() == BsonType.STRING) additionalValues.put(fieldName, reader.readString());
			//TODO etc...
			reader.readEndArray();
		}
		else { reader.skipValue(); }
	}
	
}

class UnknownCredential extends Credential {
	
	Map<String, Object> additionalValues;
	public Map<String, Object> getAdditionalValues() { return additionalValues; }
	/**
	 * Credential
	 *
	 * @param identityProvider Identity Provider
	 * @param userId           User Id
	 */
	public UnknownCredential(String identityProvider, String userId, Map<String, Object> additionalValues) {
		super(identityProvider, userId);
		this.additionalValues = additionalValues;
	}
}