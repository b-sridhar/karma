# karma
A utility to pull the Jira tickets that you have worked on for the last(n) days and send an email.

# Usage:
* The artifact karma.jar can be downloaded from the `artifact/` folder in the project.
* Create a file `.karma.properties` in the folder where `karma.jar` has been downloaded.
* Add the following settings in `json` format:
```sh
{
    "jira_username"     : "Your Jira Username",
    "jira_password"     : "Your Jira Passowrd",
    "jira_url"          : "Jira URL",
    "jira_query"        : "Jira JQL query, Example: `project = OVD AND assignee = sbommaiah AND labels in (ovd-active, ovd-support) AND ((resolutiondate > startOfDay(-7d)) OR (status in ('Blocked', 'In Acceptance', 'In Progress') AND updatedDate > startOfDay(-7d)))`",
    "smtp_host"         : "FQDN of the SMTP Host",
    "smtp_port"         : "Port of the SMTP Host",
    "email_from_address": "From email address",
    "email_to_address"  : "Recipient email address",
    "email_message"     : "Hi, Please find the update for the last week in the mail below:",
    "signature"         : "Regards, "
}
```
* Run the following command to use the utility[assuming you have the jar and the properities files in the current directory]:
```sh
    java -jar karma.jar
```