module namespace comm = "http://hl7.org/fhir"; 
declare namespace rest = "http://www.exquery.com/restxq";
declare namespace bgdm = "http://bagridb.com/bagri-xdm";

declare 
  %rest:GET
  %rest:path("/metadata")
  %rest:produces("application/fhir+xml")
  %rest:query-param("_format", "{$format}", "") 
function comm:get-conformance($format as xs:string?) as element()* {
  <Conformance xmlns="http://hl7.org/fhir"> 
    <url value="http://bagridb.com"/>
    <version value="1.1-SNAPSHOT"/>
    <name value="Bagri DB"/>
    <status value="draft"/>
    <experimental value="true"/>
    <date value="{fn:current-dateTime()}"/>
    <publisher value="Bagri Project"/>
    <kind value="instance"/>
    <fhirVersion value="DSTU2"/>
    <acceptUnknown value="both"/>
    <format value="xml"/>
    <format value="json"/>
    <format value="application/fhir+xml"/>
    <format value="application/fhir+json"/>
  </Conformance>
};


(:

<Conformance xmlns="http://hl7.org/fhir"> 
 <!-- from Resource: id, meta, implicitRules, and language -->
 <!-- from DomainResource: text, contained, extension, and modifierExtension -->
 <url value="[uri]"/><!-- 0..1 ���������� URI ��� ������ �� ��� ��������� -->
 <version value="[string]"/><!-- 0..1 ���������� ������������� ���������� ������ ��������� -->
 <name value="[string]"/><!-- 0..1 ������������ ��� ����� ��������� � ������������ -->
 <status value="[code]"/><!-- 1..1 draft | active | retired -->
 <experimental value="[boolean]"/><!-- 0..1 ���� ������ ��� ������������, �� ��� ��������� ������������� -->
 <date value="[dateTime]"/><!-- 1..1 ����(/�����) ���������� -->
 <publisher value="[string]"/><!-- 0..1 ��� �������� (����������� ��� ������� ����) -->
 <contact>  <!-- 0..* ���������� ������ �������� -->
  <name value="[string]"/><!-- 0..1 ��� ����������� ���� ��� ����� -->
  <telecom><!-- 0..* ContactPoint ���������� ���������� ����������� ���� ��� �������� --></telecom>
 </contact>
 <description value="[markdown]"/><!-- ?? 0..1 ���������������� �������� ��������� � ������������ -->
 <useContext><!-- 0..* CodeableConcept ���������� ������������� ��� ��������� ��������� ����� ��������� --></useContext>
 <requirements value="[markdown]"/><!-- 0..1 ��� ���� ��� ������ ���� ������ -->
 <copyright value="[string]"/><!-- 0..1 ����������� �� ������������� �/��� ���������� -->
 <kind value="[code]"/><!-- 1..1 instance | capability | requirements -->
 <instantiates value="[uri]"/><!-- 0..* Canonical URL of service implemented/used by software -->
 <software>  <!-- ?? 0..1 ��, ����������� ���� ���������� � ������������ -->
  <name value="[string]"/><!-- 1..1 �������� �� -->
  <version value="[string]"/><!-- 0..1 ������, �� ������� ���������������� ��� ��������� -->
  <releaseDate value="[dateTime]"/><!-- 0..1 ���� ������� ���� ������ -->
 </software>
 <implementation>  <!-- ?? 0..1 � ������ �������� ����������� ���������� -->
  <description value="[string]"/><!-- 1..1 ��������� ���� ���������� ��������� -->
  <url value="[uri]"/><!-- 0..1 ������� URL ��� ���� ����������� -->
 </implementation>
 <fhirVersion value="[id]"/><!-- 1..1 ������ FHIR, ������� ���������� ������� -->
 <acceptUnknown value="[code]"/><!-- 1..1 no | extensions | elements | both -->
 <format value="[code]"/><!-- 1..* �������������� ������� (xml | json | mime-���)  -->
 <profile><!-- 0..* Reference(StructureDefinition) �������������� ������� ��������� ������������� --></profile>
 <rest>  <!-- ?? 0..* ���� ����� �������������� �������� RESTful -->
  <mode value="[code]"/><!-- 1..1 client | server -->
  <documentation value="[string]"/><!-- 0..1 ����� �������� ���������� -->
  <security>  <!-- 0..1 ���������� � ������������ ���������� -->
   <cors value="[boolean]"/><!-- 0..1 ��������� CORS-��������� (http://enable-cors.org/) -->
   <service><!-- 0..* CodeableConcept OAuth | SMART-on-FHIR | NTLM | Basic | Kerberos | Certificates --></service>
   <description value="[string]"/><!-- 0..1 ����� �������� ����, ��� �������� ����������� ������������ -->
   <certificate>  <!-- 0..* �����������, ��������� � ��������� ������������ -->
    <type value="[code]"/><!-- 0..1 Mime type for certificate  -->
    <blob value="[base64Binary]"/><!-- 0..1 ��������������� ��� ���������� -->
   </certificate>
  </security>
  <resource>  <!-- 0..* ������, ������������� � ���� REST-���������� -->
   <type value="[code]"/><!-- 1..1 ��� �������, ������� �������������� -->
   <profile><!-- 0..1 Reference(StructureDefinition) ������� ��������� ������� ��� ���� ������������� ������� --></profile>
   <documentation value="[markdown]"/><!-- 0..1 Additional information about the use of the resource type -->
   <interaction>  <!-- 1..* ����� �������� ��������������? -->
    <code value="[code]"/><!-- 1..1 read | vread | update | delete | history-instance | history-type | create | search-type -->
    <documentation value="[string]"/><!-- 0..1 ����������� ������ �������� -->
   </interaction>
   <versioning value="[code]"/><!-- 0..1 no-version | versioned | versioned-update -->
   <readHistory value="[boolean]"/><!-- 0..1 ����� �� vRead ���������� ������� ������ -->
   <updateCreate value="[boolean]"/><!-- 0..1 ����� �� update ��������� ����� �������� -->
   <conditionalCreate value="[boolean]"/><!-- 0..1 �����������/������������ �� �������� ��������� �������� -->
   <conditionalRead value="[code]"/><!-- 0..1 not-supported | modified-since | not-match | full-support -->
   <conditionalUpdate value="[boolean]"/><!-- 0..1 �����������/������������ �� �������� ��������� ���������� -->
   <conditionalDelete value="[code]"/><!-- 0..1 not-supported | single | multiple - how conditional delete is supported -->
   <searchInclude value="[string]"/><!-- 0..* �������� _include, �������������� �������� -->
   <searchRevInclude value="[string]"/><!-- 0..* ��������� �������� ��������� _revinclude, �������������� �������� -->
   <searchParam>  <!-- 0..* ��������� ������, �������������� ����������� -->
    <name value="[string]"/><!-- 1..1 ��� ��������� ������ -->
    <definition value="[uri]"/><!-- 0..1 �������� ����������� ��������� -->
    <type value="[code]"/><!-- 1..1 number | date | string | token | reference | composite | quantity | uri -->
    <documentation value="[string]"/><!-- 0..1 ����������, ����������� ��� ������� -->
    <target value="[code]"/><!-- 0..* ���� ������� (��� ������ �� ������) -->
    <modifier value="[code]"/><!-- 0..* missing | exact | contains | not | text | in | not-in | below | above | type -->
    <chain value="[string]"/><!-- 0..* ������������� ���������� ����� -->
   </searchParam>
  </resource>
  <interaction>  <!-- 0..* ����� �������� ��������������? -->
   <code value="[code]"/><!-- 1..1 transaction | batch | search-system | history-system -->
   <documentation value="[string]"/><!-- 0..1 ����������� ������ �������� -->
  </interaction>
  <searchParam><!-- 0..* Content as for Conformance.rest.resource.searchParam ��������� ������ �� ���� �������� --></searchParam>
  <operation>  <!-- 0..* ����������� �������� ��� ����������������� ������� -->
   <name value="[string]"/><!-- 1..1 ��� ��� ������ ���� ��������/������� -->
   <definition><!-- 1..1 Reference(OperationDefinition) �������� ��������/������ --></definition>
  </operation>
  <compartment value="[uri]"/><!-- 0..* ���������� ������ (Compartments), �������������/������������ �������� -->
 </rest>
 <messaging>  <!-- ?? 0..* �������������� �� ����� ����������� -->
  <endpoint>  <!-- 0..* ���� ��������� ������ ���� ���������� -->
   <protocol><!-- 1..1 Coding http | ftp | mllp + --></protocol>
   <address value="[uri]"/><!-- 1..1 ����� ����� �������������� -->
  </endpoint>
  <reliableCache value="[unsignedInt]"/><!-- 0..1 ����� ����������� ��� ������������ ������ ����������� (���) -->
  <documentation value="[string]"/><!-- 0..1 ����������� ������ ���������� ������ ����������� -->
  <event>  <!-- 1..* �������� ��������� ����� ������� -->
   <code><!-- 1..1 Coding ��� ������� --></code>
   <category value="[code]"/><!-- 0..1 Consequence | Currency | Notification -->
   <mode value="[code]"/><!-- 1..1 sender | receiver -->
   <focus value="[code]"/><!-- 1..1 ������ � ������ �������� ��������� -->
   <request><!-- 1..1 Reference(StructureDefinition) �������, ����������� ������ --></request>
   <response><!-- 1..1 Reference(StructureDefinition) �������, ����������� ����� --></response>
   <documentation value="[string]"/><!-- 0..1 ������������ �������, ����������� ��� ����� �������������� -->
  </event>
 </messaging>
 <document>  <!-- ?? 0..* ����������� ��������� -->
  <mode value="[code]"/><!-- 1..1 producer | consumer -->
  <documentation value="[string]"/><!-- 0..1 �������� ��������� ���������� -->
  <profile><!-- 1..1 Reference(StructureDefinition) ����������� �� ������, ������������ � ��������� --></profile>
 </document>
</Conformance>

:)