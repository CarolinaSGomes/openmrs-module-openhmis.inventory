<%
    ui.decorateWith("appui", "standardEmrPage", [ title: ui.message("openhmis.inventory.admin.operations") ])

    /* load stylesheets */
    ui.includeCss("openhmis.commons", "bootstrap.css")
    ui.includeCss("openhmis.commons", "entities2x.css")
    ui.includeCss("uicommons", "ngDialog/ngDialog.min.css")
    ui.includeCss("openhmis.inventory", "entity.css")

    /* load angular libraries */
    ui.includeJavascript("uicommons", "angular.min.js")
    ui.includeJavascript("uicommons", "angular-ui/angular-ui-router.min.js")
    ui.includeJavascript("uicommons", "angular-ui/ui-bootstrap-tpls-0.11.2.min.js")
    ui.includeJavascript("uicommons", "angular-common.js")
    ui.includeJavascript("uicommons", "ngDialog/ngDialog.js")

    /* load re-usables/common modules */
    ui.includeFragment("openhmis.commons", "load.reusable.modules")

    /* load operations modules */
    ui.includeJavascript("openhmis.inventory", "stockOperations/models/entity.model.js")
    ui.includeJavascript("openhmis.inventory", "stockOperations/services/entity.restful.services.js")
    ui.includeJavascript("openhmis.inventory", "stockOperations/controllers/entity.controller.js")
    ui.includeJavascript("openhmis.inventory", "myOperations/controllers/view-entity.controller.js")
    ui.includeJavascript("openhmis.inventory", "stockOperations/services/entity.functions.js")
    ui.includeJavascript("openhmis.inventory", "constants.js")
%>

<script data-main="myOperations/configs/entity.main" src="/${ ui.contextPath() }/moduleResources/uicommons/scripts/require/require.js"></script>

<div id="myOperationsApp">
    <div ui-view></div>
</div>
