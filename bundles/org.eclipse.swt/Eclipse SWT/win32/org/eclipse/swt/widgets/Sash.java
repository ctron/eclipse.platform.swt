/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.widgets;


import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.win32.*;

/**
 * Instances of the receiver represent a selectable user interface object
 * that allows the user to drag a rubber banded outline of the sash within
 * the parent control.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>HORIZONTAL, VERTICAL, SMOOTH</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles HORIZONTAL and VERTICAL may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#sash">Sash snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Sash extends Control {
	boolean dragging;
	int startX, startY, lastX, lastY;
	final static int INCREMENT = 1;
	final static int PAGE_INCREMENT = 9;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#HORIZONTAL
 * @see SWT#VERTICAL
 * @see SWT#SMOOTH
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Sash (Composite parent, int style) {
	super (parent, checkStyle (style));
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control is selected by the user, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the x, y, width, and height fields of the event object are valid.
 * If the receiver is being dragged, the event object detail field contains the value <code>SWT.DRAG</code>.
 * <code>widgetDefaultSelected</code> is not called.
 * </p>
 *
 * @param listener the listener which should be notified when the control is selected by the user
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
public void addSelectionListener (SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Selection,typedListener);
	addListener (SWT.DefaultSelection,typedListener);
}

@Override
long /*int*/ callWindowProc (long /*int*/ hwnd, int msg, long /*int*/ wParam, long /*int*/ lParam) {
	if (handle == 0) return 0;
	return OS.DefWindowProc (hwnd, msg, wParam, lParam);
}

@Override
void createHandle () {
	super.createHandle ();
	state |= THEME_BACKGROUND;
}

static int checkStyle (int style) {
	return checkBits (style, SWT.HORIZONTAL, SWT.VERTICAL, 0, 0, 0, 0);
}

@Override
public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	wHint = (wHint != SWT.DEFAULT ? DPIUtil.autoScaleUp(wHint, getDisplay ()) : wHint);
	hHint = (hHint != SWT.DEFAULT ? DPIUtil.autoScaleUp(hHint, getDisplay ()) : hHint);
	int border = getBorderWidth ();
	int width = border * 2, height = border * 2;
	if ((style & SWT.HORIZONTAL) != 0) {
		width += DEFAULT_WIDTH;  height += 3;
	} else {
		width += 3; height += DEFAULT_HEIGHT;
	}
	if (wHint != SWT.DEFAULT) width = wHint + (border * 2);
	if (hHint != SWT.DEFAULT) height = hHint + (border * 2);
	return DPIUtil.autoScaleDown(new Point (width, height), getDisplay ());
}

void drawBand (int x, int y, int width, int height) {
	if ((style & SWT.SMOOTH) != 0) return;
	x = DPIUtil.autoScaleUp (x, getDisplay ());
	y = DPIUtil.autoScaleUp (y, getDisplay ());
	width = DPIUtil.autoScaleUp (width, getDisplay ());
	height = DPIUtil.autoScaleUp (height, getDisplay ());
	long /*int*/ hwndTrack = parent.handle;
	byte [] bits = {-86, 0, 85, 0, -86, 0, 85, 0, -86, 0, 85, 0, -86, 0, 85, 0};
	long /*int*/ stippleBitmap = OS.CreateBitmap (8, 8, 1, 1, bits);
	long /*int*/ stippleBrush = OS.CreatePatternBrush (stippleBitmap);
	long /*int*/ hDC = OS.GetDCEx (hwndTrack, 0, OS.DCX_CACHE);
	long /*int*/ oldBrush = OS.SelectObject (hDC, stippleBrush);
	OS.PatBlt (hDC, x, y, width, height, OS.PATINVERT);
	OS.SelectObject (hDC, oldBrush);
	OS.ReleaseDC (hwndTrack, hDC);
	OS.DeleteObject (stippleBrush);
	OS.DeleteObject (stippleBitmap);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is selected by the user.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Selection, listener);
	eventTable.unhook (SWT.DefaultSelection,listener);
}

@Override
TCHAR windowClass () {
	return display.windowClass;
}

@Override
long /*int*/ windowProc () {
	return display.windowProc;
}

@Override
LRESULT WM_ERASEBKGND (long /*int*/ wParam, long /*int*/ lParam) {
	super.WM_ERASEBKGND (wParam, lParam);
	drawBackground (wParam);
	return LRESULT.ONE;
}

@Override
LRESULT WM_KEYDOWN (long /*int*/ wParam, long /*int*/ lParam) {
	LRESULT result = super.WM_KEYDOWN (wParam, lParam);
	if (result != null) return result;
	switch ((int)/*64*/wParam) {
		case OS.VK_LEFT:
		case OS.VK_RIGHT:
		case OS.VK_UP:
		case OS.VK_DOWN:

			/* Calculate the new x or y position */
			if (OS.GetKeyState (OS.VK_LBUTTON) < 0) return result;
			int step = OS.GetKeyState (OS.VK_CONTROL) < 0 ? INCREMENT : PAGE_INCREMENT;
			if ((style & SWT.VERTICAL) != 0) {
				if (wParam == OS.VK_UP || wParam == OS.VK_DOWN) break;
				if (wParam == OS.VK_LEFT) step = -step;
				if ((parent.style & SWT.MIRRORED) != 0) step = -step;
			} else {
				if (wParam == OS.VK_LEFT || wParam == OS.VK_RIGHT) break;
				if (wParam == OS.VK_UP) step = -step;
			}
			RECT rect = new RECT ();
			OS.GetWindowRect (handle, rect);
			int width = rect.right - rect.left;
			int height = rect.bottom - rect.top;
			long /*int*/ hwndTrack = parent.handle;
			RECT clientRect = new RECT ();
			OS.GetClientRect (hwndTrack, clientRect);
			int clientWidth = clientRect.right - clientRect.left;
			int clientHeight = clientRect.bottom - clientRect.top;
			OS.MapWindowPoints (0, hwndTrack, rect, 2);
			POINT cursorPt = new POINT ();
			int newX = rect.left, newY = rect.top;
			if ((style & SWT.VERTICAL) != 0) {
				cursorPt.x = newX = Math.min (Math.max (clientRect.left, newX + step), clientWidth - width);
				cursorPt.y = rect.top + height / 2;
			} else {
				cursorPt.x = rect.left + width / 2;
				cursorPt.y = newY = Math.min (Math.max (clientRect.top, newY + step), clientHeight - height);
			}
			if (newX == rect.left && newY == rect.top) return result;

			/* Update the pointer position */
			OS.ClientToScreen (hwndTrack, cursorPt);
			OS.SetCursorPos (cursorPt.x, cursorPt.y);

			Event event = new Event ();
			event.x = newX;
			event.y = newY;
			event.width = width;
			event.height = height;
			sendSelectionEvent  (SWT.Selection, event, true);
			if (isDisposed ()) return LRESULT.ZERO;
			if (event.doit) {
				if ((style & SWT.SMOOTH) != 0) {
					setBounds (event.x, event.y, width, height);
				}
			}
			return result;
	}
	return result;
}

@Override
LRESULT WM_GETDLGCODE (long /*int*/ wParam, long /*int*/ lParam) {
	return new LRESULT (OS.DLGC_STATIC);
}

@Override
LRESULT WM_LBUTTONDOWN (long /*int*/ wParam, long /*int*/ lParam) {
	LRESULT result = super.WM_LBUTTONDOWN (wParam, lParam);
	if (result == LRESULT.ZERO) return result;

	/* Compute the banding rectangle */
	long /*int*/ hwndTrack = parent.handle;
	POINT pt = new POINT ();
	OS.POINTSTOPOINT (pt, lParam);
	RECT rect = new RECT ();
	OS.GetWindowRect (handle, rect);
	OS.MapWindowPoints (handle, 0, pt, 1);
	startX = pt.x - rect.left;
	startY = pt.y - rect.top;
	OS.MapWindowPoints (0, hwndTrack, rect, 2);
	lastX = rect.left;
	lastY = rect.top;
	int width = rect.right - rect.left;
	int height = rect.bottom - rect.top;

	/* The event must be sent because doit flag is used */
	Event event = new Event ();
	event.x = lastX;
	event.y = lastY;
	event.width = width;
	event.height = height;
	if ((style & SWT.SMOOTH) == 0) {
		event.detail = SWT.DRAG;
	}
	sendSelectionEvent (SWT.Selection, event, true);
	if (isDisposed ()) return LRESULT.ZERO;

	/* Draw the banding rectangle */
	if (event.doit) {
		dragging = true;
		lastX = event.x;
		lastY = event.y;
		menuShell ().bringToTop ();
		if (isDisposed ()) return LRESULT.ZERO;
		if (OS.IsWinCE) {
			OS.UpdateWindow (hwndTrack);
		} else {
			int flags = OS.RDW_UPDATENOW | OS.RDW_ALLCHILDREN;
			OS.RedrawWindow (hwndTrack, null, 0, flags);
		}
		drawBand (event.x, event.y, width, height);
		if ((style & SWT.SMOOTH) != 0) {
			setBounds (event.x, event.y, width, height);
			// widget could be disposed at this point
		}
	}
	return result;
}

@Override
LRESULT WM_LBUTTONUP (long /*int*/ wParam, long /*int*/ lParam) {
	LRESULT result = super.WM_LBUTTONUP (wParam, lParam);
	if (result == LRESULT.ZERO) return result;

	/* Compute the banding rectangle */
	if (!dragging) return result;
	dragging = false;
	RECT rect = new RECT ();
	OS.GetWindowRect (handle, rect);
	int width = rect.right - rect.left;
	int height = rect.bottom - rect.top;

	/* The event must be sent because doit flag is used */
	Event event = new Event ();
	event.x = lastX;
	event.y = lastY;
	event.width = width;
	event.height = height;
	drawBand (event.x, event.y, width, height);
	sendSelectionEvent (SWT.Selection, event, true);
	if (isDisposed ()) return result;
	if (event.doit) {
		if ((style & SWT.SMOOTH) != 0) {
			setBounds (event.x, event.y, width, height);
			// widget could be disposed at this point
		}
	}
	return result;
}

@Override
LRESULT WM_MOUSEMOVE (long /*int*/ wParam, long /*int*/ lParam) {
	LRESULT result = super.WM_MOUSEMOVE (wParam, lParam);
	if (result != null) return result;
	if (!dragging || (wParam & OS.MK_LBUTTON) == 0) return result;

	/* Compute the banding rectangle */
	POINT pt = new POINT ();
	OS.POINTSTOPOINT (pt, lParam);
	long /*int*/ hwndTrack = parent.handle;
	OS.MapWindowPoints (handle, hwndTrack, pt, 1);
	RECT rect = new RECT (), clientRect = new RECT ();
	OS.GetWindowRect (handle, rect);
	int width = rect.right - rect.left;
	int height = rect.bottom - rect.top;
	OS.GetClientRect (hwndTrack, clientRect);
	int newX = lastX, newY = lastY;
	if ((style & SWT.VERTICAL) != 0) {
		int clientWidth = clientRect.right - clientRect.left;
		newX = Math.min (Math.max (0, pt.x - startX), clientWidth - width);
	} else {
		int clientHeight = clientRect.bottom - clientRect.top;
		newY = Math.min (Math.max (0, pt.y - startY), clientHeight - height);
	}
	if (newX == lastX && newY == lastY) return result;
	drawBand (lastX, lastY, width, height);

	/* The event must be sent because doit flag is used */
	Event event = new Event ();
	event.x = newX;
	event.y = newY;
	event.width = width;
	event.height = height;
	if ((style & SWT.SMOOTH) == 0) {
		event.detail = SWT.DRAG;
	}
	sendSelectionEvent (SWT.Selection, event, true);
	if (isDisposed ()) return LRESULT.ZERO;
	if (event.doit) {
		lastX = event.x;
		lastY = event.y;
	}
	if (OS.IsWinCE) {
		OS.UpdateWindow (hwndTrack);
	} else {
		int flags = OS.RDW_UPDATENOW | OS.RDW_ALLCHILDREN;
		OS.RedrawWindow (hwndTrack, null, 0, flags);
	}
	drawBand (lastX, lastY, width, height);
	if ((style & SWT.SMOOTH) != 0) {
		setBounds (lastX, lastY, width, height);
		// widget could be disposed at this point
	}
	return result;
}

@Override
LRESULT WM_SETCURSOR (long /*int*/ wParam, long /*int*/ lParam) {
	LRESULT result = super.WM_SETCURSOR (wParam, lParam);
	if (result != null) return result;
	int hitTest = (short) OS.LOWORD (lParam);
 	if (hitTest == OS.HTCLIENT) {
	 	long /*int*/ hCursor = 0;
	 	if ((style & SWT.HORIZONTAL) != 0) {
			hCursor = OS.LoadCursor (0, OS.IDC_SIZENS);
	 	} else {
			hCursor = OS.LoadCursor (0, OS.IDC_SIZEWE);
	 	}
		OS.SetCursor (hCursor);
		return LRESULT.ONE;
	}
	return result;
}

}
