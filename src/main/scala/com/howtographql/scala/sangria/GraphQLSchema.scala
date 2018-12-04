package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import sangria.schema.{ListType, ObjectType}
import models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.schema._
import sangria.macros.derive._
import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._

object GraphQLSchema {

  implicit val GraphQLDateTime = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  implicit val authProviderEmailFormat = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat = jsonFormat1(AuthProviderSignupData)
  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail] (
    InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  lazy val AuthProviderSignupDataInputType: InputObjectType[AuthProviderSignupData] = deriveInputObjectType[AuthProviderSignupData]()

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  lazy val SuitType: ObjectType[Unit, Suit] = deriveObjectType[Unit, Suit](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("createdBy",
      Field("createdBy", UserType, resolve = c => usersFetcher.defer(c.value.createdBy))
    ),
    AddFields(
      Field("fellas", ListType(FellaType), resolve = c => fellasFetcher.deferRelSeq(fellaBySuitRel, c.value.id))
    )
  )

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    AddFields(
      Field("suits", ListType(SuitType),
        resolve = c =>  suitsFetcher.deferRelSeq(suitByUserRel, c.value.id)),
      Field("fellas", ListType(FellaType),
        resolve = c =>  fellasFetcher.deferRelSeq(fellaByUserRel, c.value.id))

    )
  )

  lazy val FellaType: ObjectType[Unit, Fella] = deriveObjectType[Unit, Fella](
    Interfaces(IdentifiableType),
    ExcludeFields("addedBy", "suitId"),
    AddFields(Field("user",  UserType, resolve = c => usersFetcher.defer(c.value.addedBy))),
    AddFields(Field("suit",  SuitType, resolve = c => suitsFetcher.defer(c.value.suitId)))
  )


  val suitByUserRel = Relation[Suit, Int]("byUser", s => Seq(s.createdBy))
  val fellaBySuitRel = Relation[Fella, Int]("bySuit", f => Seq(f.suitId))
  val fellaByUserRel = Relation[Fella, Int]("byUser", f => Seq(f.addedBy))

  val suitsFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getSuits(ids),
    (ctx: MyContext, ids: RelationIds[Suit]) => ctx.dao.getSuitsByUsersIds(ids(suitByUserRel))
  )

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val fellasFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getFellas(ids),
    (ctx: MyContext, ids: RelationIds[Fella]) => ctx.dao.getFellasByRelationIds(ids)
  )

  val Resolver = DeferredResolver.fetchers(suitsFetcher, usersFetcher, fellasFetcher)


  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allSuits", ListType(SuitType), resolve = c => c.ctx.dao.allSuits),
      Field("suit",
        OptionType(SuitType),
        arguments = Id :: Nil,
        resolve = c => suitsFetcher.deferOpt(c.arg(Id))
      ),
      Field("suits",
        ListType(SuitType),
        arguments = Ids :: Nil,
        resolve = c => suitsFetcher.deferSeq(c.arg(Ids))
      ),
      Field("allUsers", ListType(UserType), resolve = c => c.ctx.dao.allUsers),
      Field("users",
        ListType(UserType),
        arguments = List(Ids),
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("allFellas", ListType(FellaType), resolve = c => c.ctx.dao.allFellas),
      Field("fellas",
        ListType(FellaType),
        arguments = List(Ids),
        resolve = c => fellasFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val NameArg = Argument("name", StringType)
  val SurnameArg = Argument("surname", StringType)
  val AuthProviderArg = Argument("authProvider", AuthProviderSignupDataInputType)
  val TitleArg = Argument("title", StringType)
  val DescArg = Argument("description", StringType)
  val CreatedByArg = Argument("createdBy", IntType)
  val AddedByArg = Argument("addedBy", IntType)
  val SuitIdArg = Argument("suitId", IntType)
  val UserIdArg = Argument("userId", IntType)
  val EmailArg = Argument("email", StringType)
  val PasswordArg = Argument("password", StringType)
  val NickArg = Argument("nick", StringType)
  val CarArg = Argument("car", StringType)
  val RegionArg = Argument("region", StringType)
  val MusicArg = Argument("music", StringType)

  val Mutation = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field(
        "createUser",
        UserType,
        arguments = NameArg :: AuthProviderArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c.arg(NameArg), c.arg(AuthProviderArg))
      ),
      Field(
        "createSuit",
        SuitType,
        arguments = TitleArg :: DescArg :: CreatedByArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.createSuit(c.arg(TitleArg), c.arg(DescArg), c.arg(CreatedByArg))
      ),
      Field(
        "createFella",
        FellaType,
        arguments = NameArg :: SurnameArg :: NickArg :: CarArg :: RegionArg :: MusicArg :: AddedByArg :: SuitIdArg :: Nil,
        resolve = c => c.ctx.dao.createFella(
          c.arg(NameArg),
          c.arg(SurnameArg),
          c.arg(NickArg),
          c.arg(CarArg),
          c.arg(RegionArg),
          c.arg(MusicArg),
          c.arg(AddedByArg),
          c.arg(SuitIdArg)
        )
      ),
      Field(
        "login",
        UserType,
        arguments = EmailArg :: PasswordArg :: Nil,
        resolve = c => UpdateCtx(
          c.ctx.login(c.arg(EmailArg), c.arg(PasswordArg))) { user =>
            c.ctx.copy(currentUser = Some(user))
        }
      )
    )
  )

  val SchemaDefinition = Schema(QueryType, Some(Mutation))
}