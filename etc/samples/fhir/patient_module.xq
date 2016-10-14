module namespace fhir = "http://hl7.org/fhir/patient"; 
declare namespace rest = "http://www.exquery.com/restxq";
declare namespace bgdm = "http://bagridb.com/bagri-xdm";
declare namespace p = "http://hl7.org/fhir"; 


declare 
  %rest:GET
  %rest:path("/{id}")
  %rest:produces("application/fhir+xml")
(:  %rest:query-param("_format", "{$format}", "") :)
  %rest:query-param("_summary", "{$summary}", "") 
function fhir:get-patient-by-id($id as xs:string, (: $format as xs:string?, :) $summary as xs:string?) as element()? {
  collection("Patients")/p:Patient[p:id/@value = $id]
};


declare 
  %rest:GET
  %rest:path("/{id}/_history/{vid}")
  %rest:produces("application/fhir+xml")
  %rest:query-param("_format", "{$format}", "") 
function fhir:get-patient-by-id-version($id as xs:string, $vid as xs:string, $format as xs:string?) as element()? {
  collection("Patients")/p:Patient[p:id/@value = $id and p:meta/p:versionId/@value = $vid]
};



declare 
  %rest:GET
  %rest:produces("application/fhir+xml")
  %rest:matrix-param("parameters", "{$parameters}", "()")
  %rest:query-param("_format", "{$format}", "") 
function fhir:get-patients($parameters as item()*, $format as xs:string?) as element()* {

  let $itr := collection("Patients")/p:Patient 
  return
    <Bundle xmlns="http://hl7.org/fhir">
      <id value="{bgdm:get-uuid()}" />
      <meta>
        <lastUpdated value="{current-dateTime()}" />
      </meta>
      <type value="searchset" />
      <total value="{count($itr)}" />
      <link>
        <relation value="self" />
        <url value="http://bagridb.com/Patient/search?name=test" />
      </link>
      {for $doc in $itr
       return 
         <entry>
           <resource>{$doc}</resource>
         </entry>
      }
    </Bundle>
};         



declare 
  %rest:POST
  %rest:path("_search")
  %rest:produces("application/fhir+xml")
  %rest:form-param("parameters", "{$parameters}", "()")
  %rest:query-param("_format", "{$format}", "") 
function fhir:search-patients($parameters as item()*, $format as xs:string?) as element()? {
  for $doc in collection("Patients")/p:Patient
  return $doc
};



declare 
  %rest:POST
  %rest:consumes("application/fhir+xml")
  %rest:produces("application/fhir+xml")
  %rest:query-param("_format", "{$format}", "") 
function fhir:create-patient($content as xs:string, $format as xs:string?) as element()? {
  let $doc := parse-xml($content) 
  let $uri := xs:string($doc/p:Patient/p:id/@value) || ".xml"
(:  let $out := bgdm:log-output("start doc store; got uri: " || $uri, "info") :)
  let $uri := bgdm:store-document(xs:anyURI($uri), $content, ())
(:  let $out := bgdm:log-output("doc stored; got id: " || $id, "info") :)
  let $content := bgdm:get-document($uri)
  let $doc := parse-xml($content)
  return $doc/p:Patient
};


declare 
  %rest:PUT
  %rest:path("/{id}")
  %rest:consumes("application/fhir+xml")
  %rest:produces("application/fhir+xml")
  %rest:query-param("_format", "{$format}", "") 
function fhir:update-patient($id as xs:string, $content as xs:string, $format as xs:string?) as element()? {
  let $uri := bgdm:store-document(xs:anyURI($id), $content, ())
  return collection("Patients")/p:Patient[p:id/@value = $id] 
};



declare 
  %rest:DELETE
  %rest:path("/{id}")
function fhir:delete-patient($id as xs:string) as item()? {
(:  let $doc := collection("Patients")/p:Patient[p:id/@value = $id] :)
  let $uri := bgdm:remove-document(xs:anyURI($id)) 
  return ()
};


(:

����� ���������, ����������� ��� ���� ��������:
_id	token	������������� ������� (� �� ������ URL)	Resource.id
_lastUpdated	date	���� ���������� ����������. ������ ����� �� ������ ���������� ������������� ������� ��������	Resource.meta.lastUpdated
_tag	token	����� �� ���� �������	Resource.meta.tag
_profile	uri	����� ���� ��������, ���������� ��������	Resource.meta.profile
_security	token	����� �� ����� ������ ������������	Resource.meta.security
_text	string	��������� ����� �� ������������ �����	
_content	string	��������� ����� �� ����� ������� �������	
_list	string	��� ������� � ��������� ������ (�� ��������������, � �� ������� URL)	
_query	string	Custom named query	
Search Control Parameters:
���	���	��������	���������� ����������
_sort	string	������� ���������� ����������� (����� ����������� ��� ���������� �������� ����������)	��� ����������� ��������� ������
_count	number	���������� ����������� �� ��������	����� ����������
_include	string	������ ������� ��� ��������� � ���������� ������, �� ������� ��������� ��������� ��� ������ ����������	SourceType:searchParam(  :targetType)
_revinclude	string	������ ������� ��� ��������� � ���������� ������, ����� ��� ��������� �� ��������� ��� ������ ����������	SourceType:searchParam(  :targetType)
_summary	string	������ ������� ��������� �������� (��� ��������, ��� ��� ����������)	true | false (false is default)
_contained	string	���������� �� �������, ��������� � ������ ������� ��� ������ ����������	true | false | both (false is default)
_containedType	string	���������� �� ��������� ��� ������������ ������� ��� ����������� ��������� ��������	container | contained

Patient
active	token	������� �� ������ ������ � ��������	Patient.active
address	string	A server defined search that may match any of the string fields in the Address, including line, city, state, country, postalCode, and/or text	Patient.address
address-city	string	�����, ��������� � ������	Patient.address.city
address-country	string	������, ��������� � ������	Patient.address.country
address-postalcode	string	�������� ������, ��������� � ������	Patient.address.postalCode
address-state	string	����, ��������� � ������	Patient.address.state
address-use	token	��� ����������, ��������� � ������	Patient.address.use
animal-breed	token	������ ��� ���������-��������	Patient.animal.breed
animal-species	token	��� ��� ���������-��������	Patient.animal.species
birthdate	date	���� �������� ��������	Patient.birthDate
death-date	date	���� ������� ���� ������, ��� ������������� ������� �������� ��������	Patient.deceased.as(DateTime)
deceased	token	���� ������� ������� ��� �������, ���� ������� ���� ������	Patient.deceased.exists()
email	token	����� ����������� �����	Patient.telecom.where(system='email')
family	string	����� ������� ��������	Patient.name.family
gender	token	��� ��������	Patient.gender
general-practitioner	reference	Patient's nominated general practitioner, not the organization that manages the record	Patient.generalPractitioner
given	string	����� ����� ��������	Patient.name.given
identifier	token	������������� ��������	Patient.identifier
language	token	��� ����� (��������������� �������� ���� �������������)	Patient.communication.language
link	reference	��� ��������, ��������� � ������ ���������	Patient.link.other
name	string	A server defined search that may match any of the string fields in the HumanName, including family, give, prefix, suffix, suffix, and/or text	Patient.name
organization	reference	�����������, � ������� ���� ������� �������� ���������	Patient.managingOrganization
phone	token	����� ��������	Patient.telecom.where(system='phone')
phonetic	string	����� ���� �������, ���� �����, ��������� ��������� �������� ������������� ������������	Patient.name
telecom	token	�������� � ����� ���� ���������� ������ ��������	Patient.telecom
race	token	Returns patients with a race extension matching the specified code.	
ethnicity	token	Returns patients with an ethnicity extension matching the specified code.	

:)