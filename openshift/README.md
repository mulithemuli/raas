requires to pull images from a different namespace - see

```
oc policy add-role-to-user \
    system:image-puller system:serviceaccount:project-a:default \
    --namespace=project-b
```

in this special case the service account created beneath needs this role explicitly:

```
oc policy add-role-to-user \
    system:image-puller system:serviceaccount:footprint-dev:spring-boot-admin-sa \
    --namespace=footprint-builds
```

which allows pods in project-a to pull images from project-b

in that special case the deployment runs under a different service account. this account needs
the image-puller role from the -builds namespace explicitly assigned!
this can be achieved by creating a role-binding in the -builds namespace for this service-account

to create the service-account the following scripts are relevant:

```
  - kind: ServiceAccount
    apiVersion: v1
    metadata:
      name: spring-boot-admin-sa
      labels:
        app: spring-boot-admin

  - kind: Role
    apiVersion: v1
    metadata:
      name: spring-boot-admin-role
      labels:
        app: spring-boot-admin
    rules:
      - apiGroups:
          - ""
        resources:
          - pods
          - endpoints
          - services
        verbs:
          - get
          - list
          - watch

  - kind: RoleBinding
    apiVersion: authorization.openshift.io/v1
    metadata:
      name: spring-boot-admin-role-binding
      namespace: ${PROJECTNAME}
      labels:
        app: spring-boot-admin
    subjects:
      - kind: ServiceAccount
        name: spring-boot-admin-sa
        namespace: ${PROJECTNAME}
    roleRef:
      kind: Role
      name: spring-boot-admin-role
      namespace: ${PROJECTNAME}
```
