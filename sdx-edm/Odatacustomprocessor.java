package com.odata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;   
import java.util.Set;   
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.ComplexProcessor;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.serializer.ComplexSerializerOptions;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.ODataHandler;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.commons.api.http.HttpMethod; 
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.json.*;


public class Odatacustomprocessor implements Processor {

    class EData{
        public String ES_NAME;
        public FullQualifiedName ET_NAME;
        public JSONArray records;
        public EData (String ES_NAME, FullQualifiedName ET_NAME,JSONArray records) {  
         this.ET_NAME = ET_NAME;  
         this.ES_NAME = ES_NAME;
         this.records = records;  
        }
     }
     
     class DataProvider {
         private final Map<String, EntityCollection> data;
         public DataProvider(EData[] eDataArray) {
           data = new HashMap<String, EntityCollection>();
           for (int i = 0; i < eDataArray.length; i++) {
             EData eData = eDataArray[i];
           data.put(eData.ES_NAME, createData(eData.records,eData.ET_NAME));
           }
         }
       
         public EntityCollection readAll(EdmEntitySet edmEntitySet) {
           return data.get(edmEntitySet.getName());
         }
       
         public Entity read(final EdmEntitySet edmEntitySet, final List<UriParameter> keys) throws DataProviderException {
           final EdmEntityType entityType = edmEntitySet.getEntityType();
           final EntityCollection entitySet = data.get(edmEntitySet.getName());
           if (entitySet == null) {
             return null;
           } else {
             try {
               for (final Entity entity : entitySet.getEntities()) {
                 boolean found = true;
                 for (final UriParameter key : keys) {
                   final EdmProperty property = (EdmProperty) entityType.getProperty(key.getName());
                   final EdmPrimitiveType type = (EdmPrimitiveType) property.getType();
                   if (!type.valueToString(entity.getProperty(key.getName()).getValue(),
                       property.isNullable(), property.getMaxLength(), property.getPrecision(), property.getScale(),
                       property.isUnicode())
                       .equals(key.getText())) {
                     found = false;
                     break;
                   }
                 }
                 if (found) {
                   return entity;
                 }
               }
               return null;
             } catch (final EdmPrimitiveTypeException e) {
               throw new DataProviderException("Wrong key!", e);
             }
           }
         }
       
         class DataProviderException extends ODataException {
           private static final long serialVersionUID = 5098059649321796156L;
       
           public DataProviderException(String message, Throwable throwable) {
             super(message, throwable);
           }
       
           public DataProviderException(String message) {
             super(message);
           }
         }
       
         private EntityCollection createData(JSONArray records,FullQualifiedName ET_NAME) {
           EntityCollection entitySet = new EntityCollection();
           Entity el = new Entity();
       
           for(int i = 0; i < records.length(); i++)
           {
                 JSONObject object = records.getJSONObject(i);
                 JSONArray fields = object.getJSONArray("DATAFIELD");
                 el = new Entity()
                 .addProperty(createPrimitive("count", i));
                 for(int j = 0; j < fields.length(); j++) {
                    JSONObject field = fields.getJSONObject(j);
                    el.addProperty(createPrimitive(field.getString("FIELDNAME"), field.getString("FIELDVALUE")));
                 }
               entitySet.getEntities().add(el);
           }
       
           for (Entity entity:entitySet.getEntities()) {
             entity.setType(ET_NAME.getFullQualifiedNameAsString());
           }
           return entitySet;
         }
       
         private Property createPrimitive(final String name, final Object value) {
             return new Property(null, name, ValueType.PRIMITIVE, value);
           }
         
       }
     
       class ODataEntity {
         public FullQualifiedName ET_NAME;
         public String ES_NAME;
         public String[] columns;
     
        public ODataEntity (FullQualifiedName ET_NAME, String ES_NAME, String[] columns) {  
         this.ET_NAME = ET_NAME;  
         this.ES_NAME = ES_NAME;
         this.columns = columns;  
        }
     
       }
       
       class EdmProvider extends CsdlAbstractEdmProvider {
       
         // Service Namespace
         public final String NAMESPACE = "olingo.odata.customprovider";
       
         // EDM Container
         public final String CONTAINER_NAME = "Container";
         public final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
     
         public ODataEntity[] Entities;
         public EdmProvider(ODataEntity[] entities ) {
         Entities = entities;
         }
       
         @Override
         public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
     
             for (int i = 0; i < Entities.length; i++) {
                 ODataEntity entity = Entities[i];
                 if (entity.ET_NAME.equals(entityTypeName)) {
                     List<CsdlProperty> properties = new ArrayList<CsdlProperty>();
                     properties.add(new CsdlProperty().setName("count").setType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName()));
                     for (int j = 0; j < entity.columns.length; j++) {
                         String column =  entity.columns[j];
                         properties.add(new CsdlProperty().setName(column).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
                     }
                     return new CsdlEntityType()
                 .setName(entity.ET_NAME.getName())
                 .setKey(Arrays.asList(
                     new CsdlPropertyRef().setName("count")))
                 .setProperties(properties);
                 }
               }
               return null;
         }
       
       
         @Override
         public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName)
             throws ODataException {
           if (CONTAINER_FQN.equals(entityContainer)) {
             
             for (int i = 0; i < Entities.length; i++) {
                 ODataEntity entity = Entities[i];
                 if (entity.ES_NAME.equals(entitySetName)) {
               return new CsdlEntitySet()
                   .setName(entity.ES_NAME)
                   .setType(entity.ET_NAME);
                 }
             }
         
             } 
       
           return null;
         }
       
         @Override
         public List<CsdlSchema> getSchemas() throws ODataException {
           List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
           CsdlSchema schema = new CsdlSchema();
           schema.setNamespace(NAMESPACE);
     
           // EntityTypes
           List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
           
           for (int i = 0; i < Entities.length; i++) {
             ODataEntity entity = Entities[i];
             entityTypes.add(getEntityType(entity.ET_NAME));
           }
          
           schema.setEntityTypes(entityTypes);
       
           // EntityContainer
           schema.setEntityContainer(getEntityContainer());
           schemas.add(schema);
       
           return schemas;
         }
       
         @Override
         public CsdlEntityContainer getEntityContainer() throws ODataException {
           CsdlEntityContainer container = new CsdlEntityContainer();
           container.setName(CONTAINER_FQN.getName());
       
           // EntitySets
           List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
           
           container.setEntitySets(entitySets);
           for (int i = 0; i < Entities.length; i++) {
             ODataEntity entity = Entities[i];
             entitySets.add(getEntitySet(CONTAINER_FQN, entity.ES_NAME));
           }
         
           return container;
         }
       
         @Override
         public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)
                 throws ODataException {
           if (entityContainerName == null || CONTAINER_FQN.equals(entityContainerName)) {
             return new CsdlEntityContainerInfo().setContainerName(CONTAINER_FQN);
           }
           return null;
         }
       }
       
       class ODataProcessor implements EntityCollectionProcessor, EntityProcessor,
       PrimitiveProcessor, PrimitiveValueProcessor, ComplexProcessor {
       
       private OData odata;
       private final DataProvider dataProvider;
       private ServiceMetadata edm;
       
       // This constructor is application specific and not mandatory for the Olingo library. We use it here to simulate the
       // database access
       public ODataProcessor(final DataProvider dataProvider) {
       this.dataProvider = dataProvider;
       }
       
       @Override
       public void init(OData odata, ServiceMetadata edm) {
       this.odata = odata;
       this.edm = edm;
       }
       
       @Override
       public void readEntityCollection(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
         final ContentType requestedContentType) throws ODataApplicationException, SerializerException {
           
       final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
       
       EntityCollection entitySet = dataProvider.readAll(edmEntitySet);

       List<Entity> entityList = entitySet.getEntities();
        EntityCollection returnEntityCollection = new EntityCollection();

        // handle $count: return the original number of entities, ignore $top and $skip
        CountOption countOption = uriInfo.getCountOption();
        if (countOption != null) {
            boolean isCount = countOption.getValue();
            if(isCount){
                returnEntityCollection.setCount(entityList.size());
            }
        }

        // handle $skip
        SkipOption skipOption = uriInfo.getSkipOption();
        if (skipOption != null) {
            int skipNumber = skipOption.getValue();
            if (skipNumber >= 0) {
                if(skipNumber <= entityList.size()) {
                    entityList = entityList.subList(skipNumber, entityList.size());
                } else {
                    // The client skipped all entities
                    entityList.clear();
                }
            } else {
                throw new ODataApplicationException("Invalid value for $skip", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }

        // handle $top
        TopOption topOption = uriInfo.getTopOption();
        if (topOption != null) {
            int topNumber = topOption.getValue();
            if (topNumber >= 0) {
                if(topNumber <= entityList.size()) {
                    entityList = entityList.subList(0, topNumber);
                }  // else the client has requested more entities than available => return what we have
            } else {
                throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }

        // after applying the query options, create EntityCollection based on the reduced list
        for(Entity entity : entityList){
            returnEntityCollection.getEntities().add(entity);
        }
       
       // Next we create a serializer based on the requested format. This could also be a custom format but we do not
       // support them in this example
       ODataSerializer serializer = odata.createSerializer(requestedContentType);
       
       // Now the content is serialized using the serializer.
       final ExpandOption expand = uriInfo.getExpandOption();
       final SelectOption select = uriInfo.getSelectOption();
       final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
       InputStream serializedContent = serializer.entityCollection(edm, edmEntitySet.getEntityType(), returnEntityCollection,
           EntityCollectionSerializerOptions.with()
               .id(id)
               .contextURL(isODataMetadataNone(requestedContentType) ? null :
                   getContextUrl(edmEntitySet, false, expand, select, null))
               .count(uriInfo.getCountOption())
               .expand(expand).select(select)
               .build()).getContent();
       
       // Finally we set the response data, headers and status code
       response.setContent(serializedContent);
       response.setStatusCode(HttpStatusCode.OK.getStatusCode());
       response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
       }
       
       @Override
       public void readEntity(final ODataRequest request, ODataResponse response, final UriInfo uriInfo,
         final ContentType requestedContentType) throws ODataApplicationException, SerializerException {
       // First we have to figure out which entity set the requested entity is in
       final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
       
       // Next we fetch the requested entity from the database
       Entity entity;
       try {
         entity = readEntityInternal(uriInfo.asUriInfoResource(), edmEntitySet);
       } catch (DataProvider.DataProviderException e) {
         throw new ODataApplicationException(e.getMessage(), 500, Locale.ENGLISH);
       }
       
       if (entity == null) {
         // If no entity was found for the given key we throw an exception.
         throw new ODataApplicationException("No entity found for this key", HttpStatusCode.NOT_FOUND
             .getStatusCode(), Locale.ENGLISH);
       } else {
         // If an entity was found we proceed by serializing it and sending it to the client.
         ODataSerializer serializer = odata.createSerializer(requestedContentType);
         final ExpandOption expand = uriInfo.getExpandOption();
         final SelectOption select = uriInfo.getSelectOption();
         InputStream serializedContent = serializer.entity(edm, edmEntitySet.getEntityType(), entity,
             EntitySerializerOptions.with()
                 .contextURL(isODataMetadataNone(requestedContentType) ? null :
                     getContextUrl(edmEntitySet, true, expand, select, null))
                 .expand(expand).select(select)
                 .build()).getContent();
         response.setContent(serializedContent);
         response.setStatusCode(HttpStatusCode.OK.getStatusCode());
         response.setHeader(HttpHeader.CONTENT_TYPE, requestedContentType.toContentTypeString());
       }
       }
       
       @Override
       public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
                              ContentType requestFormat, ContentType responseFormat)
             throws ODataApplicationException, DeserializerException, SerializerException {
       throw new ODataApplicationException("Entity create is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
             throws ODataApplicationException {
       throw new ODataApplicationException("Entity delete is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType format)
             throws ODataApplicationException, SerializerException {
       readProperty(response, uriInfo, format, false);
       }
       
       @Override
       public void readComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType format)
             throws ODataApplicationException, SerializerException {
       readProperty(response, uriInfo, format, true);
       }
       
       @Override
       public void readPrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType format)
             throws ODataApplicationException, SerializerException {
       // First we have to figure out which entity set the requested entity is in
       final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
       // Next we fetch the requested entity from the database
       final Entity entity;
       try {
         entity = readEntityInternal(uriInfo.asUriInfoResource(), edmEntitySet);
       } catch (DataProvider.DataProviderException e) {
         throw new ODataApplicationException(e.getMessage(), 500, Locale.ENGLISH);
       }
       if (entity == null) {
         // If no entity was found for the given key we throw an exception.
         throw new ODataApplicationException("No entity found for this key", HttpStatusCode.NOT_FOUND
                 .getStatusCode(), Locale.ENGLISH);
       } else {
         // Next we get the property value from the entity and pass the value to serialization
         UriResourceProperty uriProperty = (UriResourceProperty) uriInfo
                 .getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
         EdmProperty edmProperty = uriProperty.getProperty();
         Property property = entity.getProperty(edmProperty.getName());
         if (property == null) {
           throw new ODataApplicationException("No property found", HttpStatusCode.NOT_FOUND
                   .getStatusCode(), Locale.ENGLISH);
         } else {
           if (property.getValue() == null) {
             response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
           } else {
             String value = String.valueOf(property.getValue());
             ByteArrayInputStream serializerContent = new ByteArrayInputStream(
                     value.getBytes(Charset.forName("UTF-8")));
             response.setContent(serializerContent);
             response.setStatusCode(HttpStatusCode.OK.getStatusCode());
             response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
           }
         }
       }
       }
       
       private void readProperty(ODataResponse response, UriInfo uriInfo, ContentType contentType,
         boolean complex) throws ODataApplicationException, SerializerException {
       // To read a property we have to first get the entity out of the entity set
       final EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo.asUriInfoResource());
       Entity entity;
       try {
         entity = readEntityInternal(uriInfo.asUriInfoResource(), edmEntitySet);
       } catch (DataProvider.DataProviderException e) {
         throw new ODataApplicationException(e.getMessage(),
                 HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
       }
       
       if (entity == null) {
         // If no entity was found for the given key we throw an exception.
         throw new ODataApplicationException("No entity found for this key",
                 HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
       } else {
         // Next we get the property value from the entity and pass the value to serialization
         UriResourceProperty uriProperty = (UriResourceProperty) uriInfo
             .getUriResourceParts().get(uriInfo.getUriResourceParts().size() - 1);
         EdmProperty edmProperty = uriProperty.getProperty();
         Property property = entity.getProperty(edmProperty.getName());
         if (property == null) {
           throw new ODataApplicationException("No property found",
                   HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
         } else {
           if (property.getValue() == null) {
             response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
           } else {
             ODataSerializer serializer = odata.createSerializer(contentType);
             final ContextURL contextURL = isODataMetadataNone(contentType) ? null :
                 getContextUrl(edmEntitySet, true, null, null, edmProperty.getName());
             InputStream serializerContent = complex ?
                 serializer.complex(edm, (EdmComplexType) edmProperty.getType(), property,
                     ComplexSerializerOptions.with().contextURL(contextURL).build()).getContent() :
                 serializer.primitive(edm, (EdmPrimitiveType) edmProperty.getType(), property,
                                       PrimitiveSerializerOptions.with()
                                       .contextURL(contextURL)
                                       .scale(edmProperty.getScale())
                                       .nullable(edmProperty.isNullable())
                                       .precision(edmProperty.getPrecision())
                                       .maxLength(edmProperty.getMaxLength())
                                       .unicode(edmProperty.isUnicode()).build()).getContent();
             response.setContent(serializerContent);
             response.setStatusCode(HttpStatusCode.OK.getStatusCode());
             response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
           }
         }
       }
       }
       
       private Entity readEntityInternal(final UriInfoResource uriInfo, final EdmEntitySet entitySet)
         throws DataProvider.DataProviderException {
       // This method will extract the key values and pass them to the data provider
       final UriResourceEntitySet resourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
       return dataProvider.read(entitySet, resourceEntitySet.getKeyPredicates());
       }
       
       private EdmEntitySet getEdmEntitySet(final UriInfoResource uriInfo) throws ODataApplicationException {
       final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
       /*
        * To get the entity set we have to interpret all URI segments
        */
       if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
         throw new ODataApplicationException("Invalid resource type for first segment.",
             HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       /*
        * Here we should interpret the whole URI but in this example we do not support navigation so we throw an exception
        */
       
       final UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);
       return uriResource.getEntitySet();
       }
       
       private ContextURL getContextUrl(final EdmEntitySet entitySet, final boolean isSingleEntity,
         final ExpandOption expand, final SelectOption select, final String navOrPropertyPath)
         throws SerializerException {
       
       return ContextURL.with().entitySet(entitySet)
           .selectList(odata.createUriHelper().buildContextURLSelectList(entitySet.getEntityType(), expand, select))
           .suffix(isSingleEntity ? Suffix.ENTITY : null)
           .navOrPropertyPath(navOrPropertyPath)
           .build();
       }
       
       @Override
       public void updatePrimitive(final ODataRequest request, final ODataResponse response,
                                 final UriInfo uriInfo, final ContentType requestFormat,
                                 final ContentType responseFormat)
             throws ODataApplicationException, DeserializerException, SerializerException {
       throw new ODataApplicationException("Primitive property update is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void updatePrimitiveValue(final ODataRequest request, ODataResponse response,
         final UriInfo uriInfo, final ContentType requestFormat, final ContentType responseFormat)
         throws ODataApplicationException, ODataLibraryException {
       throw new ODataApplicationException("Primitive property update is not supported yet.",
           HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws
             ODataApplicationException {
       throw new ODataApplicationException("Primitive property delete is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void deletePrimitiveValue(final ODataRequest request, ODataResponse response, final UriInfo uriInfo)
         throws ODataApplicationException, ODataLibraryException {
       throw new ODataApplicationException("Primitive property update is not supported yet.",
           HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void updateComplex(final ODataRequest request, final ODataResponse response,
                               final UriInfo uriInfo, final ContentType requestFormat,
                               final ContentType responseFormat)
             throws ODataApplicationException, DeserializerException, SerializerException {
       throw new ODataApplicationException("Complex property update is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void deleteComplex(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
             throws ODataApplicationException {
       throw new ODataApplicationException("Complex property delete is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       @Override
       public void updateEntity(final ODataRequest request, final ODataResponse response,
                              final UriInfo uriInfo, final ContentType requestFormat,
                              final ContentType responseFormat)
             throws ODataApplicationException, DeserializerException, SerializerException {
       throw new ODataApplicationException("Entity update is not supported yet.",
               HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
       }
       
       public boolean isODataMetadataNone(final ContentType contentType) {
       return contentType.isCompatible(ContentType.APPLICATION_JSON) 
          && ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(
              contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
       }
       }
       
    public void process(Exchange exchange) throws Exception {
          HashMap hash = (HashMap<String,String>)exchange.getIn().getBody();
          String workordersJson = hash.get("WORKORDERS_KEY").toString();
          String assetsJson = hash.get("ASSETS_KEY").toString();
          JSONObject workorderJSONObj = new JSONObject(workordersJson.trim());
          JSONObject assetJSONObj = new JSONObject(assetsJson.trim());
          JSONArray workOrderRecords = workorderJSONObj.getJSONObject("Result").getJSONObject("ResultData").getJSONArray("DATARECORD");
          JSONArray assetRecords = assetJSONObj.getJSONObject("Result").getJSONObject("ResultData").getJSONArray("DATARECORD");

            String NAMESPACE = "olingo.odata.customprovider";

            EData[] eData = new EData[]{new EData("workorders",new FullQualifiedName(NAMESPACE, "workorder"), workOrderRecords),
            new EData("assets",new FullQualifiedName(NAMESPACE, "asset"), assetRecords) };
            DataProvider dataProvider = null;
             if (dataProvider == null) {
               dataProvider = new DataProvider(eData);
             }

             String[] workordercolumns = {"equipment","description","schedstartdate","reportedby","organization","workordernum","duedate","workorderstatus","equipmentorg","workorderrtype","equipsystype",             "relatedfromreference","relatedtoreference","reasonforrepair","workaccomplished","techpartfailure","manufacturer","syslevel","asslevel","idmdocxpath","complevel","componentlocation"};  
             String[] assetcolumns = {"equipmentno","organization","equipmentdesc","assetstatus","description","assetstatus","commissiondate"};  

             ODataEntity[] entities = new ODataEntity[]{ new ODataEntity(new FullQualifiedName(NAMESPACE, "workorder"),"workorders",workordercolumns),
             new ODataEntity(new FullQualifiedName(NAMESPACE, "asset"),"assets", assetcolumns)};

            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(new EdmProvider(entities), new ArrayList<EdmxReference>());
            ODataHandler handler = odata.createRawHandler(edm);

            Message msg = exchange.getIn();

            handler.register(new ODataProcessor(dataProvider));

            ODataRequest oDatareq = new ODataRequest();
            oDatareq.setHeader("Content-Type","application/json");
            oDatareq.setMethod(HttpMethod.GET);
            oDatareq.setRawRequestUri(msg.getHeader("CamelHttpUrl").toString());

            String rawODataPath =msg.getHeader("CamelHttpPath").toString();
            for (int i = 0; i < 1; i++) {
               int index = rawODataPath.indexOf('/', 1);
               if (-1 == index) {
                   rawODataPath = "";
                   break;
              } else {
                rawODataPath = rawODataPath.substring(index);
               }
            }
            oDatareq.setRawODataPath(rawODataPath);

            if(msg.getHeader("CamelHttpRawQuery") != null){
                oDatareq.setRawQueryPath(msg.getHeader("CamelHttpRawQuery").toString());
            }
            oDatareq.setProtocol("http");
            ODataResponse res = handler.process(oDatareq);
            
            exchange.getIn().setHeader("Content-Type",res.getHeader("Content-Type"));
            exchange.getIn().setBody( res.getContent());
    }

}