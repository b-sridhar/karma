<html>
    <head>

    </head>
    <body style="font-family:calibri">
        <p>${message}</p>
        <br>
        <table border=1 align="left" style="table-layout:fixed;">
            <tr>
                <th>Jira Ticket</th>
                <th>Status</th>
                <th>Summary</th>
                <th>Latest Update</th>
            </tr>
            <#list tasks as task>
                <tr>
                    <td width="65"><a href="https://jira.gracenote.com/browse/${task.key}">${task.key}</a></td>
                    <td>${task.status}</td>
                    <td width="180">${task.summary}</td>
                    <td>${task.lastComment}</td>
                </tr>
            </#list>
        </table>
        <br>
        <br>
        <p>${signature}</p>
    </body>
</html>