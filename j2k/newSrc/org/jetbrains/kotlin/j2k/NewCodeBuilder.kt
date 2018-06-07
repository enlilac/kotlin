/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKBodyStub
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer

class NewCodeBuilder {

    val builder = StringBuilder()
    val printer = Printer(builder)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ABSTRACT -> "abstract class"
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
    }


    inner class Visitor : JKVisitorVoid {
        override fun visitTreeElement(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
        }

        override fun visitClass(klass: JKClass) {
            printer.print(classKindString(klass.classKind))
            builder.append(" ")
            printer.print(klass.name.value)
            if (klass.declarationList.isNotEmpty()) {
                printer.println("{")
                printer.pushIndent()
                klass.declarationList.forEach { it.accept(this) }
                printer.popIndent()
                printer.println("}")
            }
        }

        override fun visitBinaryExpression(binaryExpression: JKBinaryExpression) {
            binaryExpression.left.accept(this)
            printer.printWithNoIndent(binaryExpression.operator)
            binaryExpression.right.accept(this)
        }

        override fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression) {
            printer.printWithNoIndent(ktLiteralExpression.literal)
        }

        override fun visitTypeElement(typeElement: JKTypeElement) {
            val type = typeElement.type
            when (type) {
                is JKClassType -> (type.classReference as JKClassSymbol).fqName?.let { printer.printWithNoIndent(FqName(it).shortName().asString()) }
                else -> printer.printWithNoIndent("Unit /* TODO: ${type::class} */")
            }
            when (type.nullability) {
                Nullability.Nullable -> printer.printWithNoIndent("?")
                Nullability.Default -> printer.printWithNoIndent("? /* TODO: Default */")
                else -> {
                }
            }
        }

        override fun visitKtFunction(ktFunction: JKKtFunction) {
            printer.print("fun ", ktFunction.name.value, "(", "): ")
            ktFunction.returnType.accept(this)
            if (ktFunction.block !== JKBodyStub) {
                printer.printlnWithNoIndent("{")
                printer.pushIndent()
                ktFunction.block.accept(this)
                printer.popIndent()
                printer.printWithNoIndent("}")
            }
        }

        override fun visitBlock(block: JKBlock) {
            block.acceptChildren(this)
        }

        override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
            printer.printIndent()
            expressionStatement.expression.accept(this)
            printer.printlnWithNoIndent()
        }

        override fun visitReturnStatement(returnStatement: JKReturnStatement) {
            printer.print("return ")
            returnStatement.expression.accept(this)
            printer.printlnWithNoIndent()
        }

        override fun visitKtProperty(ktProperty: JKKtProperty) {
            // TODO: Fix this
            if (ktProperty.modifierList.modifiers.any { (it as? JKJavaModifier)?.type == JKJavaModifier.JavaModifierType.FINAL }) {
                printer.print("val")
            } else {
                printer.print("var")
            }

            printer.printWithNoIndent(" ", ktProperty.name.value, ": ")
            ktProperty.type.accept(this)
            if (ktProperty.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                ktProperty.initializer.accept(this)
            }
            printer.printlnWithNoIndent()
        }
    }

    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return builder.toString()
    }
}