package org.atlasapi.client.query;

import java.util.List;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.attribute.Attribute;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Description;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.query.Selection;

public class AnyQuery extends AtlasQuery<Description> {

    AnyQuery(ImmutableList<AtomicQuery> conjuncts, Selection selection) {
        super(conjuncts, selection);
    }
    
    public StringAttributeBuilder<Description> genre() {
        return new StringAttributeBuilder<Description>(this, Attributes.DESCRIPTION_GENRE);
    }
    
    public EnumAttributeBuilder<Description, Publisher> publisher() {
        return new EnumAttributeBuilder<Description, Publisher>(this, Attributes.DESCRIPTION_PUBLISHER);
    }

    @Override
    protected AtlasQuery<Description> copyWith(ImmutableList<AtomicQuery> conjuncts, Selection selection) {
        return new AnyQuery(conjuncts, selection);
    }

    @Override
    public List<Description> extractFrom(ContentQueryResult result) {
        return ImmutableList.<Description>builder().addAll(result.getItems()).addAll(result.getPlaylists()).build();
    }

    @Override
    protected Attribute<String> titleAttribute() {
        return Attributes.DESCRIPTION_TITLE;
    }

    @Override
    Attribute<String> uriAtttribute() {
        return Attributes.DESCRIPTION_URI;
    }

    @Override
    public String urlPrefix() {
        return "any";
    }

}