package my.school.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import my.school.config.Constants;
import my.school.interceptor.ClassInterceptor;
import my.school.kit.ParaKit;
import my.school.model.Class;
import my.school.model.School;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Page;

/**
 * ClassController
 * 
 * 班级管理
 * 
 */

public class ClassController extends Controller {

	public void index() {

		// 判断当前是否是搜索的数据进行的分页
		// 如果是搜索的数据，则跳转至search方法处理
		if (!ParaKit.isEmpty(getPara("s"))) {

			search();

			return;
		}

		int page = ParaKit.paramToInt(getPara("p"), 1);

		if (page < 1) {
			page = 1;
		}
		// 读取所有的科室信息。
		Page<Class> classList = Class.dao.paginate(page, Constants.PAGE_SIZE);
		setAttr("classList", classList);
		setAttr("searchName", "");
		setAttr("searchTuuid", "");
		setAttr("searchSuuid", "");
		setAttr("searchPage", Constants.NOT_SEARCH_PAGE);
		render("index.html");
	}

	@Before(ClassInterceptor.class)
	public void add() {

		render("add.html");
	}

	/**
	 * 搜索
	 */
	public void search() {
		if (ParaKit.isEmpty(getPara("s"))) {

			// 说明当前请求是搜索数据的post请求，并非搜索的分页请求
			// 在这里执行搜索操作，并将结果保存到缓存中

			Map<String, String> queryParams = new HashMap<String, String>();
			queryParams.put("name", getPara("name"));
			queryParams.put("tuuid", getPara("tuuid"));
			queryParams.put("suuid", getPara("suuid"));

			setSessionAttr(Constants.SEARCH_SESSION_KEY, queryParams);

		}

		int page = ParaKit.paramToInt(getPara("p"), 1);

		if (page < 1) {
			page = 1;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("from class where id > 0");

		HashMap<String, String> queryParams = getSessionAttr(Constants.SEARCH_SESSION_KEY);
		List<Object> params = new ArrayList<Object>();

		if (queryParams != null) {

			String name = queryParams.get("name");

			if (!ParaKit.isEmpty(name)) {
				sb.append(" and name like ?");
				params.add("%" + name + "%");
			}

			String tuuid = queryParams.get("tuuid");

			if (!ParaKit.isEmpty(tuuid)) {
				sb.append(" and tuuid like ?");
				params.add("%" + tuuid + "%");
			}

			String suuid = queryParams.get("suuid");

			if (!ParaKit.isEmpty(suuid)) {
				sb.append(" and suuid like ?");
				params.add("%" + suuid + "%");
			}

			setAttr("searchName", name);
			setAttr("searchTuuid", tuuid);
			setAttr("searchSuuid", suuid);
			;
			setAttr("searchPage", Constants.SEARCH_PAGE);

		}

		// 医生列表
		Page<School> schoolList = School.dao.paginate(page, Constants.PAGE_SIZE, "select *",
				sb.toString(), params.toArray());

		setAttr("schoolList", schoolList);

		render("index.html");

	}

	/**
	 * 添加/修改班级信息
	 */
	public void save() {

		Class clazz = getModel(Class.class);

		String year = clazz.getStr("year");
		String sid = String.valueOf(clazz.getInt("sid"));

		System.out.println("year: " + year);

		if (ParaKit.isEmpty(clazz.getStr("id"))) {

			// 首先找到该学校该年份所有的班级数量
			int count = Class.dao.getCount(year, sid);

			System.out.println("count: " + count);

			count += 1;
			
			String uuid = null;
			String name = null;

			if (count < 10) {
				uuid = year + "000" + count;
				name = year + "级0" + count + "班";
			} else {
				uuid = year + "00" + count;
				name = year + "级" + count + "班";
			}

			System.out.println("name: " + name + "  uuid: " + uuid);

			clazz.set("year", year);
			clazz.set("name", name);
			clazz.set("uuid", uuid);

			boolean result = clazz.save();

			if (result) {
				setAttr("status", "success");
				setAttr("action", "create");
				setAttr("name", name);
				setAttr("uuid", uuid);
			} else {
				setAttr("status", "failed");
			}

		} else {

			boolean result = clazz.update();
			if (result) {
				setAttr("status", "success");
				setAttr("status", "update");
			} else {
				setAttr("status", "failed");
			}
		}

		renderJson();
	}

	/**
	 * 跳转编辑页面
	 * 
	 */
	@Before(ClassInterceptor.class)
	public void edit() {
		int classId = getParaToInt(0);
		setAttr("class", Class.dao.findById(classId));
		render("add.html");
	}

	/**
	 * 删除科室信息
	 */
	public void delete() {
		int classId = ParaKit.paramToInt(getPara(0), -1);

		if (classId > -1) {
			if (Class.dao.deleteById(classId)) {
				renderJson("msg", "删除成功！");
			}
		} else {
			renderJson("msg", "删除失败！");
		}

	}

}
