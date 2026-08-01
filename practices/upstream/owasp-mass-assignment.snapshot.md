# owasp-mass-assignment — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:21Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r124`
**Body SHA-256 (below the `---` divider, header excluded):** f0273df12eda011d667a9028a2bdfd820a49f8fb4e24b44291411857ff0dc020

---

---
snapshot_id: owasp-mass-assignment
source: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 6084
sha: "ff6fe030db89d104e9d8c7c6c8bae9af75a6558e31d8b940c2e12f664f257939"
---

# owasp mass assignment — upstream snapshot

Source: https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html
Fetched: 2026-07-14

Mass Assignment - OWASP Cheat Sheet Series
Skip to content

# Mass Assignment Cheat Sheet¶

## Introduction¶

### Definition¶
Software frameworks sometimes allow developers to automatically bind HTTP request parameters into program code variables or objects to make using that framework easier on developers. This can sometimes cause harm.
Attackers can sometimes use this methodology to create new parameters that the developer never intended which in turn creates or overwrites new variable or objects in program code that was not intended.
This is called a Mass Assignment vulnerability.

### Alternative Names¶
Depending on the language/framework in question, this vulnerability can have several alternative names:
Mass Assignment: Ruby on Rails, NodeJS.
Autobinding: Spring MVC, ASP NET MVC.
Object injection: PHP.

### Example¶
Suppose there is a form for editing a user's account information:
<form>
     <input name="userid" type="text">
     <input name="password" type="text">
     <input name="email" text="text">
     <input type="submit">
form>
Here is the object that the form is binding to:
public class User {
   private String userid;
   private String password;
   private String email;
   private boolean isAdmin;

   //Getters & Setters
}
Here is the controller handling the request:
@RequestMapping(value = "/addUser", method = RequestMethod.POST)
public String submit(User user) {
   userService.add(user);
   return "successPage";
}
Here is the typical request:
POST /addUser
...
userid=bobbytables&password=hashedpass&[email protected]
And here is the exploit in which we set the value of the attribute isAdmin of the instance of the class User:
POST /addUser
...
userid=bobbytables&password=hashedpass&[email protected]&isAdmin=true

### Exploitability¶
This functionality becomes exploitable when:
Attacker can guess common sensitive fields.
Attacker has access to source code and can review the models for sensitive fields.
AND the object with sensitive fields has an empty constructor.

### GitHub case study¶
In 2012, GitHub was hacked using mass assignment. A user was able to upload his public key to any organization and thus make any subsequent changes in their repositories. GitHub's Blog Post.

### Solutions¶
Allow-list the bindable, non-sensitive fields.
Block-list the non-bindable, sensitive fields.
Use Data Transfer Objects (DTOs).

## General Solutions¶
An architectural approach is to create Data Transfer Objects and avoid binding input directly to domain objects. Only the fields that are meant to be editable by the user are included in the DTO.
public class UserRegistrationFormDTO {
 private String userid;
 private String password;
 private String email;

 //NOTE: isAdmin field is not present

 //Getters & Setters
}

## Language & Framework specific solutions¶

### Spring MVC¶

#### Allow-listing¶
@Controller
public class UserController
{
 @InitBinder
 public void initBinder(WebDataBinder binder, WebRequest request)
 {
 binder.setAllowedFields(["userid","password","email"]);
 }
...
}
Take a look here for the documentation.

#### Block-listing¶
@Controller
public class UserController
{
   @InitBinder
   public void initBinder(WebDataBinder binder, WebRequest request)
   {
      binder.setDisallowedFields(["isAdmin"]);
   }
...
}
Take a look here for the documentation.

### NodeJS + Mongoose¶

#### Allow-listing¶
var UserSchema = new mongoose.Schema({
 userid: String,
 password: String,
 email : String,
 isAdmin : Boolean,
});

UserSchema.statics = {
 User.userCreateSafeFields: ['userid', 'password', 'email']
};

var User = mongoose.model('User', UserSchema);

_ = require('underscore');
var user = new User(_.pick(req.body, User.userCreateSafeFields));
Take a look here for the documentation.

#### Block-listing¶
var massAssign = require('mongoose-mass-assign');

var UserSchema = new mongoose.Schema({
 userid: String,
 password: String,
 email : String,
 isAdmin : { type: Boolean, protect: true, default: false }
});

UserSchema.plugin(massAssign);

var User = mongoose.model('User', UserSchema);

/** Static method, useful for creation **/
var user = User.massAssign(req.body);

/** Instance method, useful for updating**/
var user = new User;
user.massAssign(req.body);

/** Static massUpdate method **/
var input = { userid: 'bhelx', isAdmin: 'true' };
User.update({ '_id': someId }, { $set: User.massUpdate(input) }, console.log);
Take a look here for the documentation.

### Ruby On Rails¶
Take a look here for the documentation.

### Django¶
Take a look here for the documentation.

### ASP NET¶
Take a look here for the documentation.

### PHP Laravel + Eloquent¶

#### Allow-listing¶
namespace App;

use Illuminate\Database\Eloquent\Model;

class User extends Model
{
 private $userid;
 private $password;
 private $email;
 private $isAdmin;

 protected $fillable = array('userid','password','email');
}
Take a look here for the documentation.

#### Block-listing¶
namespace App;

use Illuminate\Database\Eloquent\Model;

class User extends Model
{
 private $userid;
 private $password;
 private $email;
 private $isAdmin;

 protected $guarded = array('isAdmin');
}
Take a look here for the documentation.

### Grails¶
Take a look here for the documentation.

### Play¶
Take a look here for the documentation.

### Jackson (JSON Object Mapper)¶
Take a look here and here for the documentation.

### GSON (JSON Object Mapper)¶
Take a look here and here for the document.

### JSON-Lib (JSON Object Mapper)¶
Take a look here for the documentation.

### Flexjson (JSON Object Mapper)¶
Take a look here for the documentation.

## References and future reading¶
Mass Assignment, Rails and You

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html
HTTP status: 200 · extracted bytes: 9693 · sha256: f5bdefd4e8288e68712648e6de35b69887ab72bcac7c541f268b0de6cb8485c7
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r124`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Mass Assignment - OWASP Cheat Sheet Series Skip to content OWASP Cheat Sheet Series Mass Assignment Initializing search OWASP/CheatSheetSeries OWASP Cheat Sheet Series OWASP/CheatSheetSeries Introduction Index Alphabetical Index ASVS Index MASVS Index Proactive Controls Index Top 10 Cheatsheets Cheatsheets AI Agent Security AJAX Security AML Sanctions AI Agent Payments Abuse Case Access Control Attack Surface Analysis Authentication Authorization Authorization Regression Testing Authorization Testing Automation Automotive Security Bean Validation Bot Management and Anti-Automation Browser Extension Vulnerabilities Business Logic Security C-Based Toolchain Hardening CI CD Security Choosing and Using Security Questions Clickjacking Defense Content Security Policy Cookie Theft Mitigation Credential Stuffing Prevention Cross-Site Request Forgery Prevention Cross Site Scripting Prevention Cryptographic Storage DOM Clobbering Prevention DOM based XSS Prevention Database Security Denial of Service Dependency Graph SBOM Deserialization Django REST Framework Django Security Docker Security DotNet Security Drone Security Email Validation and Verification Error Handling File Upload Forgot Password GitHub Actions Security GraphQL HTML5 Security HTTP Headers HTTP Strict Transport Security Infrastructure as Code Security Injection Prevention Injection Prevention in Java Input Validation Insecure Direct Object Reference Prevention JAAS JSON Web Token Java Security Key Management Kubernetes Security LDAP Injection Prevention LLM Prompt Injection Prevention Laravel Legacy Application Management Logging Logging Vocabulary MCP Security Mass Assignment Mass Assignment Table of contents Introduction Definition Alternative Names Example Exploitability GitHub case study Solutions General Solutions Language & Framework specific solutions Spring MVC Allow-listing Block-listing NodeJS + Mongoose Allow-listing Block-listing Ruby On Rails Django ASP NET PHP Laravel + Eloquent Allow-listing Block-listing Grails Play Jackson (JSON Object Mapper) GSON (JSON Object Mapper) JSON-Lib (JSON Object Mapper) Flexjson (JSON Object Mapper) References and future reading Microservices Security Microservices based Security Arch Doc Mobile Application Security Multi Tenant Security Multifactor Authentication NPM Security Network Segmentation NoSQL Security NodeJS Docker Nodejs Security OAuth2 OS Command Injection Defense PHP Configuration Password Storage Pinning Prototype Pollution Prevention Query Parameterization RAG Security REST Assessment REST Security Ruby on Rails SAML Security SQL Injection Prevention Secrets Management Secure AI Model Ops Secure Cloud Architecture Secure Code Review Secure Coding with AI Secure Product Design Securing Cascading Style Sheets Security Terminology Server Side Request Forgery Prevention Serverless FaaS Security Session Management Software Supply Chain Security Subdomain Takeover Prevention Symfony TLS Cipher String Third Party Javascript Management Third Party Payment Gateway Integration Threat Modeling Transaction Authorization Transport Layer Protection Transport Layer Security Unvalidated Redirects and Forwards User Privacy Protection Virtual Patching Vulnerability Disclosure Vulnerable Dependency Management WebSocket Security Web Service Security XML External Entity Prevention XML Security XSS Filter Evasion XS Leaks Zero Trust Architecture gRPC Security Table of contents Introduction Definition Alternative Names Example Exploitability GitHub case study Solutions General Solutions Language & Framework specific solutions Spring MVC Allow-listing Block-listing NodeJS + Mongoose Allow-listing Block-listing Ruby On Rails Django ASP NET PHP Laravel + Eloquent Allow-listing Block-listing Grails Play Jackson (JSON Object Mapper) GSON (JSON Object Mapper) JSON-Lib (JSON Object Mapper) Flexjson (JSON Object Mapper) References and future reading Mass Assignment Cheat Sheet ¶ Introduction ¶ Definition ¶ Software frameworks sometimes allow developers to automatically bind HTTP request parameters into program code variables or objects to make using that framework easier on developers. This can sometimes cause harm. Attackers can sometimes use this methodology to create new parameters that the developer never intended which in turn creates or overwrites new variable or objects in program code that was not intended. This is called a Mass Assignment vulnerability. Alternative Names ¶ Depending on the language/framework in question, this vulnerability can have several alternative names : Mass Assignment: Ruby on Rails, NodeJS. Autobinding: Spring MVC, ASP NET MVC. Object injection: PHP. Example ¶ Suppose there is a form for editing a user's account information: < form > < input name = "userid" type = "text" > < input name = "password" type = "text" > < input name = "email" text = "text" > < input type = "submit" > </ form > Here is the object that the form is binding to: public class User { private String userid ; private String password ; private String email ; private boolean isAdmin ; //Getters & Setters } Here is the controller handling the request: @RequestMapping ( value = "/addUser" , method = RequestMethod . POST ) public String submit ( User user ) { userService . add ( user ); return "successPage" ; } Here is the typical request: POST /addUser ... userid=bobbytables&password=hashedpass& [email protected] And here is the exploit in which we set the value of the attribute isAdmin of the instance of the class User : POST /addUser ... userid=bobbytables&password=hashedpass& [email protected] &isAdmin=true Exploitability ¶ This functionality becomes exploitable when: Attacker can guess common sensitive fields. Attacker has access to source code and can review the models for sensitive fields. AND the object with sensitive fields has an empty constructor. GitHub case study ¶ In 2012, GitHub was hacked using mass assignment. A user was able to upload his public key to any organization and thus make any subsequent changes in their repositories. GitHub's Blog Post . Solutions ¶ Allow-list the bindable, non-sensitive fields. Block-list the non-bindable, sensitive fields. Use Data Transfer Objects (DTOs). General Solutions ¶ An architectural approach is to create Data Transfer Objects and avoid binding input directly to domain objects. Only the fields that are meant to be editable by the user are included in the DTO. public class UserRegistrationFormDTO { private String userid ; private String password ; private String email ; //NOTE: isAdmin field is not present //Getters & Setters } Language & Framework specific solutions ¶ Spring MVC ¶ Allow-listing ¶ @Controller public class UserController { @InitBinder public void initBinder ( WebDataBinder binder , WebRequest request ) { binder . setAllowedFields ( [ "userid" , "password" , "email" ] ); } ... } Take a look here for the documentation. Block-listing ¶ @Controller public class UserController { @InitBinder public void initBinder ( WebDataBinder binder , WebRequest request ) { binder . setDisallowedFields ( [ "isAdmin" ] ); } ... } Take a look here for the documentation. NodeJS + Mongoose ¶ Allow-listing ¶ var UserSchema = new mongoose . Schema ({ userid : String , password : String , email : String , isAdmin : Boolean , }); UserSchema . statics = { User . userCreateSafeFields : [ 'userid' , 'password' , 'email' ] }; var User = mongoose . model ( 'User' , UserSchema ); _ = require ( 'underscore' ); var user = new User ( _ . pick ( req . body , User . userCreateSafeFields )); Take a look here for the documentation. Block-listing ¶ var massAssign = require ( 'mongoose-mass-assign' ); var UserSchema = new mongoose . Schema ({ userid : String , password : String , email : String , isAdmin : { type : Boolean , protect : true , default : false } }); UserSchema . plugin ( massAssign ); var User = mongoose . model ( 'User' , UserSchema ); /** Static method, useful for creation **/ var user = User . massAssign ( req . body ); /** Instance method, useful for updating**/ var user = new User ; user . massAssign ( req . body ); /** Static massUpdate method **/ var input = { userid : 'bhelx' , isAdmin : 'true' }; User . update ({ '_id' : someId }, { $set : User . massUpdate ( input ) }, console . log ); Take a look here for the documentation. Ruby On Rails ¶ Take a look here for the documentation. Django ¶ Take a look here for the documentation. ASP NET ¶ Take a look here for the documentation. PHP Laravel + Eloquent ¶ Allow-listing ¶ <?php namespace App ; use Illuminate\Database\Eloquent\Model ; class User extends Model { private $userid ; private $password ; private $email ; private $isAdmin ; protected $fillable = array ( 'userid' , 'password' , 'email' ); } Take a look here for the documentation. Block-listing ¶ <?php namespace App ; use Illuminate\Database\Eloquent\Model ; class User extends Model { private $userid ; private $password ; private $email ; private $isAdmin ; protected $guarded = array ( 'isAdmin' ); } Take a look here for the documentation. Grails ¶ Take a look here for the documentation. Play ¶ Take a look here for the documentation. Jackson (JSON Object Mapper) ¶ Take a look here and here for the documentation. GSON (JSON Object Mapper) ¶ Take a look here and here for the document. JSON-Lib (JSON Object Mapper) ¶ Take a look here for the documentation. Flexjson (JSON Object Mapper) ¶ Take a look here for the documentation. References and future reading ¶ Mass Assignment, Rails and You ©Copyright - Cheat Sheets Series Team - This work is licensed under Creative Commons Attribution-ShareAlike 4.0 International . Made with Material for MkDocs
