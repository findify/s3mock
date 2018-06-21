package io.findify.s3mock.response

import com.amazonaws.services.s3.model.Tag

case class GetObjectTagging(tags: List[Tag]) {
  def toXML =
    <Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <TagSet>
        {
          tags.map(tag =>
            <Tag>
              <Key>{tag.getKey}</Key>
              <Value>{tag.getValue}</Value>
            </Tag>
          )
        }
      </TagSet>
    </Tagging>
}

