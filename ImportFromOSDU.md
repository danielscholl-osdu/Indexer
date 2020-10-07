#### The following updates need to be made when code is merged from OSDU:
1. Please update the repo names in `devops/azure/pipeline.yml` file:
```
resources:
  repositories:
  - repository: FluxRepo
    type: git
    name: r3-gitops-manifests
  - repository: TemplateRepo
    type: git
    name: r3-infra-azure-provisioning
```
2. Update the variable groups in the `devops/azure/pipeline.yml` file:

```
variables:
  - group: 'R3MVP - Azure - OSDU'
  - group: 'R3MVP - Azure - OSDU Secrets'
```
3. Update the env. names in the `devops/azure/pipeline.yml` file:
```
  providers:
    -  name: Azure
       environments: ['dev']
```
