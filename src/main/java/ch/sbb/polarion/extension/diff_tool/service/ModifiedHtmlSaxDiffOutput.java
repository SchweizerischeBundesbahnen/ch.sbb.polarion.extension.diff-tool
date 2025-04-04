/*
 * Copyright 2007 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sbb.polarion.extension.diff_tool.service;

import org.outerj.daisy.diff.html.dom.ImageNode;
import org.outerj.daisy.diff.html.dom.Node;
import org.outerj.daisy.diff.html.dom.TagNode;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.html.modification.Modification;
import org.outerj.daisy.diff.html.modification.ModificationType;
import org.outerj.daisy.diff.output.DiffOutput;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Takes a branch root and creates an HTML file for it.
 * NOTE: initial code was taken from {@link org.outerj.daisy.diff.html.HtmlSaxDiffOutput} to apply some fixes.
 */
@SuppressWarnings("all")
public class ModifiedHtmlSaxDiffOutput implements DiffOutput {

    public static final String CLASS_HTML_REMOVED = "diff-html-removed";
    public static final String CLASS_HTML_ADDED = "diff-html-added";

    private ContentHandler handler;

    private String prefix;

    public ModifiedHtmlSaxDiffOutput(ContentHandler handler, String name) {
        this.handler = handler;
        this.prefix = name;
    }

    /**
     * {@inheritDoc}
     */
    public void generateOutput(TagNode node) throws SAXException {

        if (!node.getQName().equalsIgnoreCase("img")
                && !node.getQName().equalsIgnoreCase("body")) {
            handler.startElement("", node.getQName(), node.getQName(), node
                    .getAttributes());
        }

        if (node.getQName().equals("br")) {
            handler.endElement("", node.getQName(), node.getQName());
            return;
        }

        boolean newStarted = false;
        boolean remStarted = false;
        boolean changeStarted = false;
        boolean conflictStarted = false;
        String changeTXT = "";

        for (Node child : node) {
            if (child instanceof TagNode tagNode) {
                if (tagNode.getQName().equals("br")) {
                    //do not close diff when br met
                } else if (newStarted) {
                    handler.endElement("", "span", "span");
                    newStarted = false;
                } else if (changeStarted) {
                    handler.endElement("", "span", "span");
                    changeStarted = false;
                } else if (remStarted) {
                    handler.endElement("", "span", "span");
                    remStarted = false;
                } else if (conflictStarted) {
                	handler.endElement("", "span", "span");
                	conflictStarted = false;
                }
                generateOutput(((TagNode) child));
            } else if (child instanceof TextNode) {
                TextNode textChild = (TextNode) child;
                Modification mod = textChild.getModification();

                if (newStarted
                        && (mod.getOutputType() != ModificationType.ADDED || mod
                                .isFirstOfID())) {
                    handler.endElement("", "span", "span");
                    newStarted = false;
                } else if (changeStarted
                        && (mod.getOutputType() != ModificationType.CHANGED
                                || !mod.getChanges().equals(changeTXT) || mod
                                .isFirstOfID())) {
                    handler.endElement("", "span", "span");
                    changeStarted = false;
                } else if (remStarted
                        && (mod.getOutputType() != ModificationType.REMOVED || mod
                                .isFirstOfID())) {
                    handler.endElement("", "span", "span");
                    remStarted = false;
                } else if (conflictStarted
                        && (mod.getOutputType() != ModificationType.CONFLICT || mod
                                .isFirstOfID())) {
                    handler.endElement("", "span", "span");
                    conflictStarted = false;
                }

                // no else because a removed part can just be closed and a new
                // part can start
                if (!newStarted && mod.getOutputType() == ModificationType.ADDED) {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("", "class", "class", "CDATA",
                            CLASS_HTML_ADDED);
                    if (mod.isFirstOfID()) {
                        attrs.addAttribute("", "id", "id", "CDATA", mod
                                .getOutputType()
                                + "-" + prefix + "-" + mod.getID());
                    }

                    addAttributes(mod, attrs);

                    handler.startElement("", "span", "span", attrs);
                    newStarted = true;
                } else if (!changeStarted
                        && mod.getOutputType() == ModificationType.CHANGED) {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("", "class", "class", "CDATA",
                            "diff-html-changed");
                    if (mod.isFirstOfID()) {
                        attrs.addAttribute("", "id", "id", "CDATA", mod
                                .getOutputType()
                                + "-" + prefix + "-" + mod.getID());
                    }

                    addAttributes(mod, attrs);
                    handler.startElement("", "span", "span", attrs);

                    changeStarted = true;
                    changeTXT = mod.getChanges();
                } else if (!remStarted
                		&& mod.getOutputType() == ModificationType.REMOVED) {
                	AttributesImpl attrs = new AttributesImpl();
                	attrs.addAttribute("", "class", "class", "CDATA",
                	CLASS_HTML_REMOVED);
                	if (mod.isFirstOfID()) {
                		attrs.addAttribute("", "id", "id", "CDATA", mod
                				.getOutputType()
                				+ "-" + prefix + "-" + mod.getID());
                	}
                	addAttributes(mod, attrs);

                	handler.startElement("", "span", "span", attrs);
                	remStarted = true;
                } else if (!conflictStarted
                        && mod.getOutputType() == ModificationType.CONFLICT) {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("", "class", "class", "CDATA",
                            "diff-html-conflict");
                    if (mod.isFirstOfID()) {
                        attrs.addAttribute("", "id", "id", "CDATA", mod
                                .getOutputType()
                                + "-" + prefix + "-" + mod.getID());
                    }
                    addAttributes(mod, attrs);

                    handler.startElement("", "span", "span", attrs);
                    conflictStarted = true;
                }

                char[] chars = textChild.getText().toCharArray();

                if (textChild instanceof ImageNode) {
                    writeImage((ImageNode)textChild);
                } else {
                    handler.characters(chars, 0, chars.length);
                }

            }
        }

        if (newStarted) {
            handler.endElement("", "span", "span");
            newStarted = false;
        } else if (changeStarted) {
            handler.endElement("", "span", "span");
            changeStarted = false;
        } else if (remStarted) {
            handler.endElement("", "span", "span");
            remStarted = false;
        } else if (conflictStarted) {
        	handler.endElement("", "span", "span");
        	conflictStarted = false;
        }

        if (!node.getQName().equalsIgnoreCase("img")
                && !node.getQName().equalsIgnoreCase("body"))
            handler.endElement("", node.getQName(), node.getQName());

    }

    private void writeImage(ImageNode imgNode) throws SAXException {
        AttributesImpl attrs = imgNode.getAttributes();
        if (imgNode.getModification().getOutputType() == ModificationType.REMOVED) {
            attrs.addAttribute("", "changeType", "changeType", "CDATA",
                    "diff-removed-image");
        } else if (imgNode.getModification().getOutputType() == ModificationType.ADDED) {
            attrs.addAttribute("", "changeType", "changeType", "CDATA",
                    "diff-added-image");
        } else if (imgNode.getModification().getOutputType() == ModificationType.CONFLICT) {
        	attrs.addAttribute("", "changeType", "changeType", "CDATA",
        	"diff-conflict-image");
        }
        handler.startElement("", "img", "img", attrs);
        handler.endElement("", "img", "img");
    }

    private void addAttributes(Modification mod, AttributesImpl attrs) {

        if (mod.getOutputType() == ModificationType.CHANGED) {
            String changes = mod.getChanges();
            attrs.addAttribute("", "changes", "changes", "CDATA", changes);
        }

        String previous;
        if (mod.getPrevious() == null) {
            previous = "first-" + prefix;
        } else {
            previous = mod.getPrevious().getOutputType() + "-" + prefix + "-"
                    + mod.getPrevious().getID();
        }
        attrs.addAttribute("", "previous", "previous", "CDATA", previous);

        String changeId = mod.getOutputType() + "-" + prefix + "-" + mod.getID();
        attrs.addAttribute("", "changeId", "changeId", "CDATA", changeId);

        String next;
        if (mod.getNext() == null) {
            next = "last-" + prefix;
        } else {
            next = mod.getNext().getOutputType() + "-" + prefix + "-"
                    + mod.getNext().getID();
        }
        attrs.addAttribute("", "next", "next", "CDATA", next);

    }

}