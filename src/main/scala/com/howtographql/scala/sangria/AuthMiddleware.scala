package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Authorized
import sangria.execution.{Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.{Action, Context}

object AuthMiddleware extends Middleware[MyContext] with MiddlewareBeforeField[MyContext] {
  override type QueryVal = Unit
  override type FieldVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[MyContext, _, _]) = ()

  override def afterQuery(queryVal: Unit, context: MiddlewareQueryContext[MyContext, _, _]) = ()

  override def beforeField(queryVal: Unit, mctx: MiddlewareQueryContext[MyContext, _, _], ctx: Context[MyContext, _]) ={
    val requireAuth = ctx.field.tags contains Authorized

    if (requireAuth) ctx.ctx.ensureAuthenticated()

    continue
  }
}