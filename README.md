# Keycloak Extended User Search

Web-service for searching Keycloak users by user attributes and groups.

> **Disclaimer**. The main goal of this project is not to make module that will solve all possible problems, but to create an example that others can easily extend to their needs.

## Background

Users in Keycloak can have custom attributes and can be organized via groups.
Sometimes we need to search users that are in group `N` and have attribute `A` that like `%value%`.

[Example 1](https://stackoverflow.com/questions/54667407/how-to-get-users-by-custom-attributes-in-keycloak),
[Example 2](https://lists.jboss.org/pipermail/keycloak-user/2016-December/008896.html).

This web-service tries to implement this kind of search for [Keycloak.4.5.0.Final](https://github.com/keycloak/keycloak/tree/4.5.0.Final). I'm sorry for this unpopular version, but in 2017-2018 this version was "mainstream", and later we stopped development around Keycloak at all.

## Short descriptions before we start

Working sequence with service is very simple:

* get Bearer Token
* search with GET-query eg `http://127.0.0.1:8080/auth/realms/test/user-search/?email=%25gmail.com&group=admin%7C%7Cmoderator&somecustomattribute=premium`
* ???
* PROFIT

## Build

```sh
mvn install
```

## Install

> This kind of service can't be installed as plugin and should be installed as `module`.

### Install via script

```sh
$KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=io.github.andrewsha256.keycloak_user_search --resources=target/keycloak-user-search.jar --dependecies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private,org.keycloak.keycloak-services,javax.ws.rs.api"
```

### Manual install

```sh
mkdir -p $KEYCLOAK_HOME/modules/io/github/andrewsha256/keycloak-user-search/main
cp target/keycloak-user-search.jar $KEYCLOAK_HOME/modules/io/github/andrewsha256/keycloak-user-search/main/keycloak-user-search.jar
cp module $KEYCLOAK_HOME/modules/io/github/andrewsha256/keycloak-user-search/main/module.xml
```

## Module Registration

In file `$KEYCLOAK_HOME/standalone/configuration/standalone.xml` add provider `module:io.github.andrewsha256.keycloak_user_search`:

```xml
<subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
    <web-context>auth</web-context>
    <providers>
        <provider>
            classpath:${jboss.home.dir}/providers/*
        </provider>
        ...
        <provider>
            module:io.github.andrewsha256.keycloak_user_search
        </provider>
    </providers>
    ...
</subsystem>
```

> Don't forget to restart Keycloak after all.

## Working with service

### Web point

After installation we have 3 new points to work with:

* `/auth/realms/:realmId/user-search/info` — "Hello world" page,
* `/auth/realms/:realmId/user-search/` — user search,
* `/auth/realms/:realmId/user-search/index` — user index.

To change root URL of service which is `user-search` by default you have to change `public static final String ID = "user-search";` constant in `UserResourceProviderFactory` class (`src/main/io/github/andrewsha256/keycloak-user-search/rest/UserResourceProviderFactory.java`).

### Search Query

Full `Search Query` consists of `Search Terms`.

`Search Term` consists of `Search Field` and `Search Value`.

`Search Value` can be `Single String Value` or `List of String Values` with `||` delimiter that will be concatenated with `OR`.

`Single String Value` has `Equals` comparator by default: eg `some value`, `Starts` comparator if it has `%` symbol at beginning, `Ends` if `%` is in the end and `Contains` comparator if it's surrounded by `%`: eg `%ome val%`.

All `Search Terms` are concatenated with `AND`.

It's possible to specify one `Search Field` multiple times: `group=administrator&group=guest`.

For instance, we need to find all users with `mail` on `gmail` server from `moderators` or `admin` `group`, and who have attribute with name `type` and value `premium`.

```URL
?email=%25gmail.com&group=admin%7C%7Cmoderator&type=premium
```

> Search is **register-independent**.

### Sorting and limiting results

It is possible to add sort to results via arguments `_orderBy=<searchField>` and `_orderDirect=<ASC|DESC>`.

It is very useful to limit result to "portions" with `_first` and `_max` arguments. `_first` starts with `0`.

```URL
?email=%25gmail.com&group=admin%7C%7Cmoderator&type=premium&_orderBy=username&_max=15_orderDirect=ASC
```

### Index

Index has 2 additional arguments: `_label` and `_value`. `_value` sets filter of label values.

Filtering index is similar to search.

```URL
?_label=username&_value=B&email=%25gmail.com&group=admin%7C%7Cmoderator&type=premium&_orderBy=username&_max=15
```

### Service Response

Service responses are simple.

Usually it's JSON with 2 properties:

* `totalSize` — total amount of users that matching query,
* `users` — array of users.

### Permissions

You need user with `Client Role` `realm-management > query-users` for having ability to request service.

You need additional role `view-users` to get users data.

[Screenshot](https://github.com/andrewsha256/andrewsha256.github.io/blob/master/repositories/keycloak-user-search/user-permissions.jpg)

### Error handling

By default external module error handling is not too verbose, so you need to check HTTP status codes:

* `400` — Bad Request,
* `403` — Forbidden for user's Bearer Token.

### Service semantics in details

More about the semantics of the service you can read in `src/main/io/github/andrewsha256/keycloak-user-search/rest/UserResource.java`.

## Thanks to

This work could not be done without googling same projects, so I would like to thank people who shared them on GitHub:

* [@pavelbogomolenko](https://github.com/pavelbogomolenko/) for his [keycloak-custom-password-hash](https://github.com/pavelbogomolenko/keycloak-custom-password-hash),
* [@dteleguin](https://github.com/dteleguin) for [beercloak](https://github.com/dteleguin/beercloak).

And notably [@afronkin](https://github.com/afronkin) for patience and helping me IRL (especially with optimizing SQL-queries).
