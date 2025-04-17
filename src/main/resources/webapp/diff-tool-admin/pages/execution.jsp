<%@ page import="ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.stream.IntStream" %>
<%@ page import="java.util.List" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.util.OSUtils" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! Version version = ExtensionInfo.getInstance().getVersion();%>
<%! int maxThreadsCount = OSUtils.getMaxRecommendedParallelThreads();%>

<head>
    <title>Execution Queue Management Panel</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <script type="module" src="../js/modules/execution.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <script type="text/javascript" src="../js/chart.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <script type="text/javascript" src="../js/chartjs-adapter-date-fns.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <script type="text/javascript" src="../js/hammer.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <script type="text/javascript" src="../js/chartjs-plugin-zoom.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <style type="text/css">

        .chart-container {
            width: 100%;
        }

        .chart-expand-container {
            width: 100%;
            height: 250px;
        }

        .footer-item {
            float: right;
            margin-right: 20px;
            display: inline-block;
        }

        .workers-configuration-table {
            display: inline-block;
            vertical-align: top;
            margin: 20px;
        }

        .workers-configuration-table table th {
            font-weight: bold;
        }

        .workers-configuration-table table td {
            padding: 10px;
        }

        .workers-configuration-table table td .number-input {
            -webkit-appearance: none; margin: 0;
            appearance: textfield;
            width: 90px;
        }

        .has-delimiter {
            margin-left: 0;
            border-left: 1px solid lightgray;
        }

        .chart-header > * {
            display: inline-block;
        }

        .chart-expand-button {
            color: #004D73;
            font-weight: bold;
            font-size: 20px;
            margin-bottom: 10px;
            padding: 0;
            border-color: transparent;
            background-color: transparent;
            text-align: center;
            vertical-align: middle;
            width: 30px;
            cursor: pointer;
        }

        .chart-expand-button:hover {
            font-size: 26px;
        }

        .center-column-value {
            text-align: center;
        }

        .more-info {
            background: url(/polarion/ria/images/msginfo.png) no-repeat;
            display: inline-block;
            width: 17px;
            height: 17px;
            cursor: pointer;
            margin-left: 5px;
            margin-bottom: -3px;
        }
    </style>
</head>

<body>

<div class="standard-admin-page">
    <h1>Execution Queue Management Panel</h1>

    <jsp:include page='/common/jsp/notifications.jsp'/>

    <div class="chart-containers">
        <%
            for (String worker : List.of("1", "2", "3", "4", "5", "6", "7", "8", "CPU_LOAD")) {
                out.println("<div class='chart-container' style='display: none' id='chart-container-" + worker + "'>" +
                        "  <div class='chart-header'>" +
                        "    <h3>" + (worker.equals("CPU_LOAD") ? "CPU Load" : "Worker-" + worker) + "</h3>" +
                        "    <button class='chart-expand-button' id='chart-expand-button-" + worker + "' title='Expand/Collapse' onclick='toggleChartContainer(`" + worker + "`)'>-</button>" +
                        "  </div>" +
                        "  <div class='chart-expand-container' id='chart-expand-container-" + worker + "' data-expanded='true'>" +
                        "    <canvas id='chart-" + worker + "'></canvas>" +
                        "    <div class='chart-footer'>" +
                        "      <div class='footer-item'>" +
                        "        <label for='select-interval-" + worker + "'>Show data within the last:</label>&nbsp;" +
                        "        <select id='select-interval-" + worker + "' title='The lesser interval is the better productivity.'>" +
                        "            <option value='1'>1 min</option>" +
                        "            <option value='3'>3 min</option>" +
                        "            <option value='5'>5 min</option>" +
                        "            <option value='15'>15 min</option>" +
                        "            <option value='30'>30 min</option>" +
                        "        </select>" +
                        "      </div>" +
                        "      <a href='javascript:void(0);' id='reset-chart-" + worker + "' class='footer-item'>Reset position</a>" +
                        "    </div>" +
                        "  </div>" +
                        "</div>");
            }
        %>
    </div>

    <script type="text/javascript">
      function toggleChartContainer(workerId) {
        const container = document.getElementById('chart-expand-container-' + workerId);
        const button = document.getElementById('chart-expand-button-' + workerId);

        const isExpanded = container.getAttribute('data-expanded') === 'true';

        if (isExpanded) {
          container.style.display = 'none';
          container.setAttribute('data-expanded', 'false');
          button.textContent = '+';
        } else {
          container.style.display = 'block';
          container.setAttribute('data-expanded', 'true');
          button.textContent = '-';
        }
      }
    </script>

    <h2>Workers Configuration</h2>

    <div class="input-container">
        <div class="input-block wide workers-configuration-container">
            <div class="workers-configuration-table" id="features-workers">
                <table>
                    <tr>
                        <th>Feature</th>
                        <th>Current Worker</th>
                        <th>Assign new</th>
                    </tr>
                    <%
                        for (Feature feature : Feature.workerFeatures()) {
                            out.println("<tr>" +
                                    "  <td><span id='feature-" + feature.name() + "'>" + feature.name() + "</span><div class='more-info' id='feature-more-info-" + feature.name() + "'></div></td>" +
                                    "  <td id='current-worker-" + feature.name() + "' class='center-column-value'> - </td>" +
                                    "  <td>" +
                                    "<select id='new-worker-" + feature.name() + "'>" +
                                    "<option value=''>--- Choose a new worker ---</option>" +
                                    "<option value='0'>Skip Queue</option>" +
                                    IntStream.rangeClosed(1, Feature.workerFeatures().size())
                                            .boxed()
                                            .map(String::valueOf).map(n -> "<option value='" + n + "'>" + n + "</option>").collect(Collectors.joining(" ")) +
                                    "</select>" +
                                    "</td>" +
                                    "</tr>");
                        }
                    %>
                </table>
            </div>
            <div class="workers-configuration-table has-delimiter" id="workers-threads">
                <table>
                    <tr>
                        <th>Worker</th>
                        <th>Current Threads Count</th>
                        <th>Assign a new value</th>
                    </tr>
                    <%
                        for (int i = 1; i <= Feature.workerFeatures().size(); i++) {
                            out.println("<tr id='worker-settings-" + i + "'>" +
                                    "  <td>Worker-" + i + "</td>" +
                                    "  <td id='current-threads-" + i + "' class='center-column-value'> - </td>" +
                                    "  <td>" +
                                    "<input type='number' class='number-input' id='new-threads-" + i + "' placeholder='A new value' min='1' max='" + maxThreadsCount + "' title='Max threads count: " + maxThreadsCount + "'/>" +
                                    "</td>" +
                                    "</tr>");
                        }
                    %>
                </table>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

<div class="standard-admin-page">
    <h2>Quick Help</h2>

    <div class="quick-help-text">
        <h3>Workers</h3>
        <p>Worker is a combination of a queue and its own executor. Current queue limit: <b>1000</b> entries.</p>
        <p>Executor fetches entries from the queue and processes them in parallel mode (if threads count for particular worker > 1).</p>
        <p>You can point specific endpoint call (AKA on this page as <b>Feature</b>) to a specific worker. This gives the opportunity to fine-tune CPU consumption.</p>
        <h3>Threads count</h3>
        <p>Max threads count depends on CPU cores/logical processors count.</p>
        <p>Be careful setting high values for several workers because this can lead to high system resources consumption.</p>
    </div>
</div>

</body>
</html>
